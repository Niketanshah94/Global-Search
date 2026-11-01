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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.feature.settings.R
import com.quickfind.feature.settings.SwitchRow
import com.quickfind.feature.settings.SystemLinks
import com.quickfind.feature.settings.toPreference
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
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

/** Three skippable screens (spec 15): value + widget, sources, default assistant. */
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
                Surface(Modifier.fillMaxSize()) {
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

    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val contactsGranted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }
    val isAssistant = remember(tick) { SystemLinks.isDefaultAssistant(context) }

    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(contactsEnabled = true) }
    }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        tick++
        if (result.values.any { it }) viewModel.enableFiles()
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                when (page) {
                    0 -> {
                        Title(stringResource(R.string.onboarding_1_title))
                        Body(stringResource(R.string.onboarding_1_text))
                        if (SystemLinks.canPinWidget(context)) {
                            Button(onClick = { SystemLinks.requestPinWidget(context) }) { Text(stringResource(R.string.add_widget)) }
                        }
                    }
                    1 -> {
                        Title(stringResource(R.string.onboarding_2_title))
                        Body(stringResource(R.string.onboarding_2_text))
                        SwitchRow(stringResource(R.string.source_apps), null, s.appsEnabled) { on -> viewModel.update { it.copy(appsEnabled = on) } }
                        SwitchRow(stringResource(R.string.source_contacts), stringResource(R.string.disclosure_contacts_text), s.contactsEnabled) { on ->
                            when {
                                !on -> viewModel.update { it.copy(contactsEnabled = false) }
                                contactsGranted -> viewModel.update { it.copy(contactsEnabled = true) }
                                else -> contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                        SwitchRow(stringResource(R.string.source_files), stringResource(R.string.disclosure_files_text), s.filesEnabled) { on ->
                            when {
                                !on -> viewModel.update { it.copy(filesEnabled = false) }
                                FilePermissions.hasMediaAccess(context) -> viewModel.enableFiles()
                                else -> mediaLauncher.launch(FilePermissions.mediaPermissions())
                            }
                        }
                    }
                    else -> {
                        Title(stringResource(R.string.onboarding_3_title))
                        Body(stringResource(R.string.onboarding_3_text))
                        OutlinedButton(
                            enabled = !isAssistant,
                            onClick = { (context as? ComponentActivity)?.let(SystemLinks::openAssistantSettings) },
                        ) {
                            Text(stringResource(if (isAssistant) R.string.set_assistant_done else R.string.set_assistant))
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onDone) { Text(stringResource(R.string.skip)) }
            Button(onClick = {
                if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onDone()
            }) {
                Text(stringResource(if (pager.currentPage < 2) R.string.next else R.string.done))
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(24.dp))
}
