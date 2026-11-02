package com.quickfind.feature.settings.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.indexing.FilePermissions
import com.quickfind.core.indexing.IndexScheduler
import com.quickfind.core.ui.Aurora
import com.quickfind.core.ui.AuroraBackground
import com.quickfind.core.ui.GlowBorderBox
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.core.ui.rememberReducedMotion
import com.quickfind.feature.settings.R
import com.quickfind.feature.settings.SystemLinks
import com.quickfind.feature.settings.toPreference
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val scheduler: IndexScheduler,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    fun enableFiles() {
        viewModelScope.launch {
            repository.update { it.copy(filesEnabled = true) }
            scheduler.requestFullScan()
        }
    }

    fun finish(then: () -> Unit) {
        viewModelScope.launch {
            repository.update { it.copy(onboardingDone = true) }
            then()
        }
    }
}

/** Three skippable screens (spec 15): value + widget, sources, open from anywhere. */
@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {
    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val s by viewModel.settings.collectAsStateWithLifecycle()
            QuickFindTheme(preference = s.themeMode.toPreference(), dynamicColor = s.dynamicColor) {
                // Back counts as "skip": onboarding should not come back on the next open.
                BackHandler { viewModel.finish(::finish) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OnboardingScreen(viewModel, s, onDone = { viewModel.finish(::finish) })
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(viewModel: OnboardingViewModel, s: UserSettings, onDone: () -> Unit) {
    val context = LocalContext.current
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val contactsGranted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }
    val calendarGranted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
    val isAssistant = remember(tick) { SystemLinks.isDefaultAssistant(context) }

    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(contactsEnabled = true) }
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(calendarEnabled = true) }
    }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        tick++
        if (result.values.any { it }) viewModel.enableFiles()
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(Modifier.fillMaxSize(), animated = !reducedMotion)
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PageDots(count = 3, current = pager.currentPage)
                TextButton(onClick = onDone) { Text(stringResource(R.string.skip)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (page) {
                        0 -> {
                            SearchOrb(animated = !reducedMotion)
                            GradientTitle(stringResource(R.string.onboarding_1_title))
                            Body(stringResource(R.string.onboarding_1_text))
                            DemoSearchBar(animated = !reducedMotion)
                            if (SystemLinks.canPinWidget(context)) {
                                OutlinedButton(onClick = { SystemLinks.requestPinWidget(context) }, shape = CircleShape) {
                                    Text(stringResource(R.string.add_widget))
                                }
                            }
                        }
                        1 -> {
                            GradientTitle(stringResource(R.string.onboarding_2_title))
                            Body(stringResource(R.string.onboarding_2_text))
                            SourceCard("📱", stringResource(R.string.source_apps), stringResource(R.string.onboarding_apps_hint), s.appsEnabled) { on ->
                                viewModel.update { it.copy(appsEnabled = on) }
                            }
                            SourceCard("👤", stringResource(R.string.source_contacts), stringResource(R.string.disclosure_contacts_text), s.contactsEnabled) { on ->
                                when {
                                    !on -> viewModel.update { it.copy(contactsEnabled = false) }
                                    contactsGranted -> viewModel.update { it.copy(contactsEnabled = true) }
                                    else -> contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            }
                            SourceCard("🗂", stringResource(R.string.source_files), stringResource(R.string.disclosure_files_text), s.filesEnabled) { on ->
                                when {
                                    !on -> viewModel.update { it.copy(filesEnabled = false) }
                                    FilePermissions.hasMediaAccess(context) -> viewModel.enableFiles()
                                    else -> mediaLauncher.launch(FilePermissions.mediaPermissions())
                                }
                            }
                            SourceCard("📅", stringResource(R.string.source_calendar), stringResource(R.string.source_calendar_summary), s.calendarEnabled) { on ->
                                when {
                                    !on -> viewModel.update { it.copy(calendarEnabled = false) }
                                    calendarGranted -> viewModel.update { it.copy(calendarEnabled = true) }
                                    else -> calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                            }
                        }
                        else -> {
                            GradientTitle(stringResource(R.string.onboarding_3_title))
                            Body(stringResource(R.string.onboarding_3_text))
                            EntryCard(
                                emoji = "✦",
                                title = stringResource(if (isAssistant) R.string.set_assistant_done else R.string.set_assistant),
                                text = stringResource(R.string.set_assistant_summary),
                                enabled = !isAssistant,
                                onClick = { (context as? ComponentActivity)?.let(SystemLinks::openAssistantSettings) },
                            )
                            EntryCard("▭", stringResource(R.string.add_widget), stringResource(R.string.add_widget_summary), SystemLinks.canPinWidget(context)) {
                                SystemLinks.requestPinWidget(context)
                            }
                            EntryCard("⚡", stringResource(R.string.onboarding_tile_title), stringResource(R.string.onboarding_tile_text), enabled = false) {}
                        }
                    }
                }
            }
            val last = pager.currentPage == 2
            GlowButton(
                text = stringResource(if (last) R.string.onboarding_start else R.string.next),
                animated = !reducedMotion,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }
        }
    }
}

/** Active page is a gradient pill, others are dots; width animates between them. */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, label = "dot")
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .then(
                        if (i == current) {
                            Modifier.background(Aurora.horizontal())
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                        },
                    ),
            )
        }
    }
}

/** The hero: a glowing ring with a breathing search glyph inside. */
@Composable
private fun SearchOrb(animated: Boolean) {
    val scale = if (animated) {
        val t = rememberInfiniteTransition(label = "orb")
        t.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
            label = "orb-scale",
        ).value
    } else {
        1f
    }
    GlowBorderBox(
        shape = CircleShape,
        borderWidth = 4.dp,
        animated = animated,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier.size(150.dp).scale(scale),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A fake search bar that types example queries and shows the answer, to teach by showing. */
@Composable
private fun DemoSearchBar(animated: Boolean) {
    val examples = stringArrayResource(R.array.onboarding_demo)
    var shown by remember { mutableStateOf(if (animated) "" else examples.first().substringBefore('|')) }
    var answer by remember { mutableStateOf(if (animated) "" else examples.first().substringAfter('|')) }
    if (animated) {
        LaunchedEffect(Unit) {
            var i = 0
            while (true) {
                val (query, result) = examples[i % examples.size].split('|', limit = 2)
                answer = ""
                for (n in 1..query.length) {
                    shown = query.take(n)
                    delay(85)
                }
                delay(250)
                answer = result
                delay(1_600)
                i++
            }
        }
    }
    GlowBorderBox(
        shape = CircleShape,
        animated = animated,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(shown + if (animated) "▏" else "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
            AnimatedVisibility(answer.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "→ $answer",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SourceCard(emoji: String, title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiTile(emoji)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun EntryCard(emoji: String, title: String, text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiTile(emoji)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmojiTile(emoji: String) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(30)).background(Brush.linearGradient(listOf(Aurora.Violet, Aurora.Magenta))),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

/** Primary button: gradient fill inside the animated glow ring. */
@Composable
private fun GlowButton(text: String, animated: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlowBorderBox(shape = CircleShape, animated = animated, containerColor = Color.Transparent, modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Aurora.horizontal())
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GradientTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge.copy(brush = Aurora.horizontal(), fontWeight = FontWeight.ExtraBold),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
