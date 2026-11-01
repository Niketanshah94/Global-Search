package com.quickfind.feature.search.folder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.quickfind.core.data.Destinations
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.ThemeMode
import com.quickfind.core.model.SearchResult
import com.quickfind.core.sources.apps.AppIconLoader
import com.quickfind.core.sources.files.FileSource
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.core.ui.ThemePreference
import com.quickfind.feature.search.ActionExecutor
import com.quickfind.feature.search.R
import com.quickfind.feature.search.ui.ResultActionSheet
import com.quickfind.feature.search.ui.ResultIconView
import com.quickfind.feature.search.ui.ResultRow
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val fileSource: FileSource,
    savedState: SavedStateHandle,
) : ViewModel() {
    val path: String = savedState.get<String>(Destinations.EXTRA_FOLDER_PATH).orEmpty()
    val title: String = savedState.get<String>(Destinations.EXTRA_FOLDER_TITLE) ?: path

    private val _items = MutableStateFlow<List<SearchResult>?>(null)
    val items: StateFlow<List<SearchResult>?> = _items

    init {
        viewModelScope.launch { _items.value = fileSource.folderContents(path) }
    }
}

/**
 * Lists one folder from our own index (spec 5.3). Opening folders in third-party file
 * managers varies too much between devices to rely on.
 */
@AndroidEntryPoint
class FolderActivity : ComponentActivity() {
    @Inject lateinit var executor: ActionExecutor
    @Inject lateinit var iconLoader: AppIconLoader
    @Inject lateinit var settings: SettingsRepository

    private val viewModel: FolderViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val s by settings.settings.collectAsStateWithLifecycle()
            val items by viewModel.items.collectAsStateWithLifecycle()
            var sheetFor by remember { mutableStateOf<SearchResult?>(null) }
            QuickFindTheme(
                preference = when (s.themeMode) {
                    ThemeMode.SYSTEM -> ThemePreference.SYSTEM
                    ThemeMode.LIGHT -> ThemePreference.LIGHT
                    ThemeMode.DARK -> ThemePreference.DARK
                },
                dynamicColor = s.dynamicColor,
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(viewModel.title) },
                            navigationIcon = {
                                IconButton(onClick = ::finish) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_up))
                                }
                            },
                        )
                    },
                ) { padding ->
                    val list = items
                    when {
                        list == null -> CircularProgressIndicator(Modifier.padding(padding).padding(24.dp))
                        list.isEmpty() -> Text(stringResource(R.string.folder_empty), Modifier.padding(padding).padding(24.dp))
                        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                            items(list, key = { it.id }) { r ->
                                ResultRow(
                                    result = r,
                                    onClick = { executor.execute(this@FolderActivity, r.primaryAction) },
                                    onLongClick = { sheetFor = r },
                                    icon = { res, m -> ResultIconView(res, iconLoader, m) },
                                )
                            }
                        }
                    }
                }
                sheetFor?.let { r ->
                    ResultActionSheet(
                        result = r,
                        isPinned = false,
                        showPinAndHide = false,
                        onAction = { a ->
                            sheetFor = null
                            executor.execute(this@FolderActivity, a)
                        },
                        onTogglePin = {},
                        onHide = {},
                        onDismiss = { sheetFor = null },
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, relativePath: String, title: String): Intent =
            Intent(context, FolderActivity::class.java)
                .putExtra(Destinations.EXTRA_FOLDER_PATH, relativePath)
                .putExtra(Destinations.EXTRA_FOLDER_TITLE, title)
    }
}
