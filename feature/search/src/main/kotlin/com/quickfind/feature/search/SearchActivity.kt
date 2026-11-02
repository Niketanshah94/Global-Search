package com.quickfind.feature.search

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.quickfind.core.data.Destinations
import com.quickfind.core.data.settings.SearchBarPosition
import com.quickfind.core.data.settings.ThemeMode
import com.quickfind.core.indexing.IndexScheduler
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.core.sources.apps.AppIconLoader
import com.quickfind.core.sources.engine.SearchEngine
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.core.ui.ThemePreference
import com.quickfind.feature.search.ui.NicknameDialog
import com.quickfind.feature.search.ui.ResultActionSheet
import com.quickfind.feature.search.ui.ResultIconView
import com.quickfind.feature.search.ui.SearchActions
import com.quickfind.feature.search.ui.SearchScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The search overlay (spec 6). Reached from the launcher icon (activity-alias MainActivity),
 * the widget, the Quick Settings tile, the notification, static shortcuts and the ASSIST
 * gesture. singleTask + translucent theme + stateAlwaysVisible keyboard are set in the manifest.
 */
@AndroidEntryPoint
class SearchActivity : ComponentActivity() {
    @Inject lateinit var executor: ActionExecutor
    @Inject lateinit var iconLoader: AppIconLoader
    @Inject lateinit var engine: SearchEngine
    @Inject lateinit var indexScheduler: IndexScheduler

    private val viewModel: SearchViewModel by viewModels()

    /** Bumped every time the screen is shown, to re-focus the field and raise the keyboard. */
    private val focusTicks = MutableStateFlow(0)
    private var checkClipboardOnFocus = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyIntent(intent)

        lifecycleScope.launch {
            if (savedInstanceState == null && !viewModel.loadSettings().onboardingDone) {
                startActivity(Intent(Destinations.ACTION_ONBOARDING).setPackage(packageName))
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val focusTick by focusTicks.collectAsStateWithLifecycle()
            val focusRequester = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current
            val view = LocalView.current
            var sheetFor by remember { mutableStateOf<SearchResult?>(null) }
            var nicknameFor by remember { mutableStateOf<SearchResult?>(null) }

            LaunchedEffect(focusTick) {
                // requestFocus throws if the field is not attached yet; it will be on the next tick.
                runCatching { focusRequester.requestFocus() }
                keyboard?.show()
            }

            QuickFindTheme(
                preference = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> ThemePreference.SYSTEM
                    ThemeMode.LIGHT -> ThemePreference.LIGHT
                    ThemeMode.DARK -> ThemePreference.DARK
                },
                dynamicColor = settings.dynamicColor,
            ) {
                val actions = remember(view) {
                    object : SearchActions {
                        override fun onQueryChange(text: String) = viewModel.onQueryChange(text)
                        override fun onSubmit() {
                            viewModel.topHit()?.let { perform(it, it.primaryAction, view) }
                        }
                        override fun onResultClick(result: SearchResult) = perform(result, result.primaryAction, view)
                        override fun onResultLongClick(result: SearchResult) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            sheetFor = result
                        }
                        override fun onAction(result: SearchResult, action: ResultAction) = perform(result, action, view)
                        override fun onToggleSection(section: ResultSection) = viewModel.toggleSection(section)
                        override fun onClearScope() = viewModel.setScope(null)
                        override fun onPasteClipboard() = paste()
                        override fun onDismissClipboard() = viewModel.dismissClipboard()
                        override fun onOpenSettings() {
                            startActivity(Intent(Destinations.ACTION_SETTINGS).setPackage(packageName))
                        }
                        override fun onDismiss() = finish()
                    }
                }
                SearchScreen(
                    queryText = viewModel.queryText,
                    state = state,
                    barAtBottom = settings.searchBarPosition == SearchBarPosition.BOTTOM,
                    actions = actions,
                    focusRequester = focusRequester,
                    icon = { result, modifier -> ResultIconView(result, iconLoader, modifier) },
                )
                sheetFor?.let { result ->
                    val instant = result.type in INSTANT_TYPES
                    ResultActionSheet(
                        result = result,
                        isPinned = result.id in state.pinnedIds,
                        showPinAndHide = !instant,
                        onAction = { action ->
                            sheetFor = null
                            perform(result, action, view)
                        },
                        onTogglePin = {
                            sheetFor = null
                            viewModel.togglePin(result)
                        },
                        onHide = {
                            sheetFor = null
                            viewModel.hide(result)
                        },
                        onDismiss = { sheetFor = null },
                        nickname = state.nicknames[result.id],
                        onEditNickname = {
                            sheetFor = null
                            nicknameFor = result
                        },
                    )
                }
                nicknameFor?.let { result ->
                    NicknameDialog(
                        result = result,
                        current = state.nicknames[result.id],
                        onSave = { nick ->
                            nicknameFor = null
                            viewModel.setNickname(result, nick)
                        },
                        onDismiss = { nicknameFor = null },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        engine.onVisible()
        indexScheduler.onScreenStarted()
        viewModel.refreshEmptyState()
        checkClipboardOnFocus = true
        focusTicks.value++
    }

    override fun onStop() {
        indexScheduler.onScreenStopped()
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        if (viewModel.settings.value.clearQueryOnReopen) viewModel.clear()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (viewModel.settings.value.clearQueryOnReopen) viewModel.clear()
        applyIntent(intent)
        focusTicks.value++
    }

    /**
     * Android 10+ only lets a focused window touch the clipboard. We only check *whether* it
     * holds text (no content read, so no Android 12 "pasted" toast); content is read on tap.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || !checkClipboardOnFocus) return
        checkClipboardOnFocus = false
        val cm = getSystemService(ClipboardManager::class.java)
        val description = if (cm.hasPrimaryClip()) cm.primaryClipDescription else null
        val isText = description != null && (
            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) ||
                description.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)
            )
        viewModel.onClipboardAvailable(isText)
    }

    private fun paste() {
        val cm = getSystemService(ClipboardManager::class.java)
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text.isNullOrBlank()) viewModel.dismissClipboard() else viewModel.onPaste(text)
    }

    private fun applyIntent(intent: Intent?) {
        val scope = when (intent?.getStringExtra(Destinations.EXTRA_SCOPE)) {
            Destinations.SCOPE_FILES -> SearchScope.FILES
            Destinations.SCOPE_CONTACTS -> SearchScope.CONTACTS
            Destinations.SCOPE_APPS -> SearchScope.APPS
            else -> null
        }
        viewModel.setScope(scope)
    }

    private fun perform(result: SearchResult, action: ResultAction, view: View) {
        val outcome = executor.execute(this, action)
        if (outcome == ActionOutcome.FAILED) return
        // A light tick when something launches (spec 7).
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        viewModel.onLaunched(result)
        if (outcome == ActionOutcome.LAUNCHED) finish()
    }

    private companion object {
        val INSTANT_TYPES = setOf(
            com.quickfind.core.model.SourceType.CALCULATOR,
            com.quickfind.core.model.SourceType.TOOL,
            com.quickfind.core.model.SourceType.WEB,
            com.quickfind.core.model.SourceType.URL,
            com.quickfind.core.model.SourceType.PHONE_NUMBER,
        )
    }
}
