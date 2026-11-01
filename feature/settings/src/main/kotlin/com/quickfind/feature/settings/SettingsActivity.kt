package com.quickfind.feature.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickfind.core.data.settings.ThemeMode
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.core.ui.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val s by viewModel.settings.collectAsStateWithLifecycle()
            QuickFindTheme(preference = s.themeMode.toPreference(), dynamicColor = s.dynamicColor) {
                SettingsScreen(viewModel = viewModel, onClose = ::finish)
            }
        }
    }
}

fun ThemeMode.toPreference(): ThemePreference = when (this) {
    ThemeMode.SYSTEM -> ThemePreference.SYSTEM
    ThemeMode.LIGHT -> ThemePreference.LIGHT
    ThemeMode.DARK -> ThemePreference.DARK
}
