package com.quickfind.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.quickfind.core.data.db.FileDao
import com.quickfind.core.data.db.HistoryDao
import com.quickfind.core.data.db.IndexMetaDao
import com.quickfind.core.data.db.QuickFindDatabase
import com.quickfind.core.data.db.ResultPrefsDao
import com.quickfind.core.data.history.HistoryRepository
import com.quickfind.core.data.history.RoomHistoryRepository
import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.data.results.RoomResultPrefsRepository
import com.quickfind.core.data.settings.DataStoreSettingsRepository
import com.quickfind.core.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): QuickFindDatabase =
        Room.databaseBuilder(context, QuickFindDatabase::class.java, QuickFindDatabase.NAME)
            // The index is rebuildable and history is a convenience: never block startup on a migration.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun fileDao(db: QuickFindDatabase): FileDao = db.fileDao()
    @Provides fun historyDao(db: QuickFindDatabase): HistoryDao = db.historyDao()
    @Provides fun resultPrefsDao(db: QuickFindDatabase): ResultPrefsDao = db.resultPrefsDao()
    @Provides fun indexMetaDao(db: QuickFindDatabase): IndexMetaDao = db.indexMetaDao()

    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)

    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO
    @Provides @DefaultDispatcher fun default(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun history(impl: RoomHistoryRepository): HistoryRepository
    @Binds abstract fun resultPrefs(impl: RoomResultPrefsRepository): ResultPrefsRepository
}
