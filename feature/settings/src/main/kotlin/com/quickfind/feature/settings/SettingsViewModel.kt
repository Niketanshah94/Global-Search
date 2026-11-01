package com.quickfind.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickfind.core.data.db.FileDao
import com.quickfind.core.data.db.HiddenResultEntity
import com.quickfind.core.data.history.HistoryRepository
import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.indexing.FileIndexer
import com.quickfind.core.indexing.IndexScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val history: HistoryRepository,
    private val resultPrefs: ResultPrefsRepository,
    private val indexer: FileIndexer,
    private val scheduler: IndexScheduler,
    fileDao: FileDao,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings

    val hidden: StateFlow<List<HiddenResultEntity>> =
        resultPrefs.hidden.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val indexedFiles: StateFlow<Int> =
        fileDao.fileCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    /** Changes that alter what is indexed need a full rescan. */
    fun updateAndReindex(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            repository.update(transform)
            scheduler.requestFullScan()
        }
    }

    fun reindex() = scheduler.requestFullScan()

    fun clearHistory() {
        viewModelScope.launch { history.clear() }
    }

    fun deleteIndex() {
        viewModelScope.launch {
            scheduler.cancelAll()
            indexer.clear()
        }
    }

    fun unhide(id: String) {
        viewModelScope.launch { resultPrefs.unhide(id) }
    }
}
