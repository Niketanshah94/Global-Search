package com.quickfind.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.matching.ranking.RankingMode
import com.quickfind.core.matching.ranking.RankingWeights
import com.quickfind.core.model.SourceType
import com.quickfind.core.model.WebSearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    /** Starts with defaults and updates once DataStore has loaded. */
    val settings: StateFlow<UserSettings>

    /** Waits for the stored values (unlike [settings], which starts with defaults). */
    suspend fun load(): UserSettings

    suspend fun update(transform: (UserSettings) -> UserSettings)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope,
) : SettingsRepository {

    override val settings: StateFlow<UserSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map(::read)
        .stateIn(scope, SharingStarted.Eagerly, UserSettings())

    override suspend fun load(): UserSettings = read(
        try {
            dataStore.data.first()
        } catch (_: IOException) {
            emptyPreferences()
        },
    )

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs ->
            val next = transform(read(prefs))
            write(prefs, next)
        }
    }

    private object Keys {
        val apps = booleanPreferencesKey("source_apps")
        val contacts = booleanPreferencesKey("source_contacts")
        val files = booleanPreferencesKey("source_files")
        val settings = booleanPreferencesKey("source_settings")
        val calculator = booleanPreferencesKey("source_calculator")
        val web = booleanPreferencesKey("source_web")
        val workApps = booleanPreferencesKey("show_work_apps")
        val engine = stringPreferencesKey("search_engine")
        val customTemplate = stringPreferencesKey("custom_search_template")
        val barPosition = stringPreferencesKey("search_bar_position")
        val theme = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val clearOnReopen = booleanPreferencesKey("clear_query_on_reopen")
        val notification = booleanPreferencesKey("persistent_notification")
        val directCalling = booleanPreferencesKey("direct_calling")
        val rankingMode = stringPreferencesKey("ranking_mode")
        val indexingPaused = booleanPreferencesKey("indexing_paused")
        val indexHidden = booleanPreferencesKey("index_hidden_folders")
        val excluded = stringSetPreferencesKey("excluded_folders")
        val trees = stringSetPreferencesKey("document_tree_uris")
        val onboarding = booleanPreferencesKey("onboarding_done")
        fun bias(type: SourceType) = intPreferencesKey("type_bias_${type.name.lowercase()}")
    }

    private val biasTypes = listOf(SourceType.APP, SourceType.CONTACT, SourceType.SETTING, SourceType.FILE)

    private fun read(p: Preferences): UserSettings {
        val d = UserSettings()
        val bias = RankingWeights.DEFAULT_TYPE_BIAS.toMutableMap()
        for (t in biasTypes) p[Keys.bias(t)]?.let { bias[t] = it }
        bias[SourceType.FOLDER] = bias[SourceType.FILE] ?: 0
        return UserSettings(
            appsEnabled = p[Keys.apps] ?: d.appsEnabled,
            contactsEnabled = p[Keys.contacts] ?: d.contactsEnabled,
            filesEnabled = p[Keys.files] ?: d.filesEnabled,
            settingsEnabled = p[Keys.settings] ?: d.settingsEnabled,
            calculatorEnabled = p[Keys.calculator] ?: d.calculatorEnabled,
            webEnabled = p[Keys.web] ?: d.webEnabled,
            showWorkApps = p[Keys.workApps] ?: d.showWorkApps,
            searchEngine = enumOr(p[Keys.engine], d.searchEngine),
            customSearchTemplate = p[Keys.customTemplate] ?: d.customSearchTemplate,
            searchBarPosition = enumOr(p[Keys.barPosition], d.searchBarPosition),
            themeMode = enumOr(p[Keys.theme], d.themeMode),
            dynamicColor = p[Keys.dynamicColor] ?: d.dynamicColor,
            clearQueryOnReopen = p[Keys.clearOnReopen] ?: d.clearQueryOnReopen,
            persistentNotification = p[Keys.notification] ?: d.persistentNotification,
            directCalling = p[Keys.directCalling] ?: d.directCalling,
            rankingMode = enumOr(p[Keys.rankingMode], d.rankingMode),
            typeBias = bias,
            indexingPaused = p[Keys.indexingPaused] ?: d.indexingPaused,
            indexHiddenFolders = p[Keys.indexHidden] ?: d.indexHiddenFolders,
            excludedFolders = p[Keys.excluded] ?: d.excludedFolders,
            documentTreeUris = p[Keys.trees] ?: d.documentTreeUris,
            onboardingDone = p[Keys.onboarding] ?: d.onboardingDone,
        )
    }

    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, s: UserSettings) {
        p[Keys.apps] = s.appsEnabled
        p[Keys.contacts] = s.contactsEnabled
        p[Keys.files] = s.filesEnabled
        p[Keys.settings] = s.settingsEnabled
        p[Keys.calculator] = s.calculatorEnabled
        p[Keys.web] = s.webEnabled
        p[Keys.workApps] = s.showWorkApps
        p[Keys.engine] = s.searchEngine.name
        p[Keys.customTemplate] = s.customSearchTemplate
        p[Keys.barPosition] = s.searchBarPosition.name
        p[Keys.theme] = s.themeMode.name
        p[Keys.dynamicColor] = s.dynamicColor
        p[Keys.clearOnReopen] = s.clearQueryOnReopen
        p[Keys.notification] = s.persistentNotification
        p[Keys.directCalling] = s.directCalling
        p[Keys.rankingMode] = s.rankingMode.name
        for (t in biasTypes) p[Keys.bias(t)] = s.typeBias[t] ?: 0
        p[Keys.indexingPaused] = s.indexingPaused
        p[Keys.indexHidden] = s.indexHiddenFolders
        p[Keys.excluded] = s.excludedFolders
        p[Keys.trees] = s.documentTreeUris
        p[Keys.onboarding] = s.onboardingDone
    }

    private inline fun <reified E : Enum<E>> enumOr(value: String?, default: E): E =
        value?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default
}
