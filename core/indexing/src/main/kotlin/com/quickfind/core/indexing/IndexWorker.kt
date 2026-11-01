package com.quickfind.core.indexing

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs the indexer under WorkManager constraints only (spec 9: no wakelocks). */
@HiltWorker
class IndexWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val indexer: FileIndexer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        if (inputData.getBoolean(KEY_FULL, false)) indexer.fullScan() else indexer.quickScan()
        Result.success()
    } catch (_: SecurityException) {
        // Permission revoked mid-scan; the next run sees the new state.
        Result.success()
    }

    companion object {
        const val KEY_FULL = "full"
        fun input(full: Boolean) = workDataOf(KEY_FULL to full)
    }
}
