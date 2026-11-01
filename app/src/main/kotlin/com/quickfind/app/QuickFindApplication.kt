package com.quickfind.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.quickfind.app.notification.SearchNotificationController
import com.quickfind.core.indexing.IndexScheduler
import com.quickfind.core.sources.engine.SearchEngine
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuickFindApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var engine: SearchEngine
    @Inject lateinit var indexScheduler: IndexScheduler
    @Inject lateinit var notifications: SearchNotificationController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Loads the app-list snapshot and resolves settings shortcuts in the background,
        // so the first keystroke already has data (spec 9).
        engine.start()
        indexScheduler.start()
        notifications.start()
    }
}
