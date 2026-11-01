package com.quickfind.core.indexing

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Index triggers (spec 5.3):
 * - a MediaStore ContentObserver while the search screen is visible (debounced 5 s),
 * - a periodic job every 6 hours with battery-not-low,
 * - a quick incremental scan whenever the app opens.
 *
 * Note: some OEMs (notably Xiaomi HyperOS) defer or kill WorkManager jobs aggressively. The
 * on-open quick scan is the fallback that keeps the index fresh even if periodic work never runs.
 */
@Singleton
class IndexScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val workManager get() = WorkManager.getInstance(context)
    private var observerJob: Job? = null
    private var observerRegistered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            observerJob?.cancel()
            observerJob = scope.launch {
                delay(OBSERVER_DEBOUNCE_MS)
                requestQuickScan()
            }
        }
    }

    /** Call once from Application.onCreate. */
    fun start() {
        scope.launch {
            settings.settings
                .map { it.filesEnabled && !it.indexingPaused }
                .distinctUntilChanged()
                .collect { active -> if (active) schedulePeriodic() else cancelAll() }
        }
        // Turning files on (after the initial value) deserves an immediate full index.
        scope.launch {
            settings.settings.map { it.filesEnabled }.distinctUntilChanged().drop(1).collect { on ->
                if (on) requestFullScan()
            }
        }
    }

    fun onScreenStarted() {
        if (!settings.settings.value.filesEnabled) return
        requestQuickScan()
        if (!observerRegistered) {
            context.contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
            observerRegistered = true
        }
    }

    fun onScreenStopped() {
        if (observerRegistered) {
            context.contentResolver.unregisterContentObserver(observer)
            observerRegistered = false
        }
        observerJob?.cancel()
    }

    fun requestQuickScan() {
        val request = OneTimeWorkRequestBuilder<IndexWorker>()
            .setInputData(IndexWorker.input(full = false))
            .build()
        workManager.enqueueUniqueWork(WORK_QUICK, ExistingWorkPolicy.KEEP, request)
    }

    /** After permission changes, folder picks or exclusions. */
    fun requestFullScan() {
        val request = OneTimeWorkRequestBuilder<IndexWorker>()
            .setInputData(IndexWorker.input(full = true))
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        workManager.enqueueUniqueWork(WORK_FULL, ExistingWorkPolicy.REPLACE, request)
    }

    private fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<IndexWorker>(6, TimeUnit.HOURS)
            .setInputData(IndexWorker.input(full = true))
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(WORK_PERIODIC)
        workManager.cancelUniqueWork(WORK_QUICK)
        workManager.cancelUniqueWork(WORK_FULL)
    }

    private companion object {
        const val WORK_PERIODIC = "file-index-periodic"
        const val WORK_QUICK = "file-index-quick"
        const val WORK_FULL = "file-index-full"
        const val OBSERVER_DEBOUNCE_MS = 5_000L
    }
}
