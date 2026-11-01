package com.quickfind.core.sources.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.NormalizedText
import com.quickfind.core.matching.TextNormalizer
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultIcon
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.SearchSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext

/** What we persist per launchable activity (snapshot for instant cold starts, spec 9). */
@Serializable
internal data class AppRecord(
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
    val label: String,
    val isWork: Boolean,
    val isDisabled: Boolean,
    val isSystem: Boolean,
)

private class AppEntry(val record: AppRecord) {
    val id = "app:${record.packageName}/${record.activityName}#${record.userSerial}"
    val label: NormalizedText = TextNormalizer.normalize(record.label)
    val pkg: NormalizedText = TextNormalizer.normalize(record.packageName)
}

/**
 * Launchable apps across all profiles (spec 5.1). In memory, scanned on every keystroke.
 *
 * No QUERY_ALL_PACKAGES: LauncherApps returns launcher activities, which the manifest's
 * `<queries>` block (ACTION_MAIN + CATEGORY_LAUNCHER) makes visible.
 */
@Singleton
class AppSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val iconLoader: AppIconLoader,
    private val json: Json,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SearchSource {
    override val type = SourceType.APP
    override val kind = SourceKind.IN_MEMORY
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.appsEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val snapshotFile = File(context.filesDir, "apps_snapshot.json")
    private val snapshotSerializer = ListSerializer(AppRecord.serializer())

    @Volatile private var entries: List<AppEntry> = emptyList()
    @Volatile private var byId: Map<String, AppEntry> = emptyMap()
    private var reloadJob: Job? = null

    override fun start() {
        scope.launch(io) {
            if (entries.isEmpty()) loadSnapshot()
            reload()
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(context, profileReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val showWork = settings.settings.value.showWorkApps
        val scored = ArrayList<Pair<Int, SearchResult>>()
        val snapshot = entries
        for ((i, e) in snapshot.withIndex()) {
            if (i and 63 == 0) currentCoroutineContext().ensureActive()
            if (e.record.isWork && !showWork) continue
            val label = FuzzyMatcher.match(query, e.label)
            val score: Int
            val ranges: List<IntRange>
            if (label != null) {
                score = label.score
                ranges = label.ranges
            } else {
                val pkg = FuzzyMatcher.match(query, e.pkg) ?: continue
                score = pkg.score + PACKAGE_MATCH_PENALTY
                ranges = emptyList()
            }
            scored += score to e.toResult(score, ranges)
        }
        return scored.sortedByDescending { it.first }.take(limit).map { it.second }
    }

    override fun resultForId(id: String): SearchResult? = byId[id]?.toResult(0, emptyList())

    private fun AppEntry.toResult(score: Int, ranges: List<IntRange>): SearchResult {
        val r = record
        val secondary = buildList {
            add(ResultAction.AppDetails(r.packageName, r.userSerial))
            // Uninstall and Play Store only make sense for user apps in the personal profile.
            if (!r.isWork && !r.isSystem) {
                add(ResultAction.Uninstall(r.packageName))
                add(ResultAction.PlayStore(r.packageName))
            }
        }
        return SearchResult(
            id = id,
            type = SourceType.APP,
            title = r.label,
            subtitle = null,
            matchScore = score,
            matchedRanges = ranges,
            primaryAction = ResultAction.LaunchApp(r.packageName, r.activityName, r.userSerial),
            secondaryActions = secondary,
            icon = ResultIcon.App(r.packageName, r.activityName, r.userSerial),
            isWorkProfile = r.isWork,
            isDisabled = r.isDisabled,
        )
    }

    private fun loadSnapshot() {
        val records = runCatching { json.decodeFromString(snapshotSerializer, snapshotFile.readText()) }.getOrNull() ?: return
        publish(records)
    }

    private fun reload() {
        val me = Process.myUserHandle()
        val records = ArrayList<AppRecord>()
        for (user in userManager.userProfiles) {
            val isWork = user != me
            // A paused work profile still lists its apps; they cannot launch until resumed.
            val quiet = isWork && runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)
            val serial = userManager.getSerialNumberForUser(user)
            val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in activities) {
                val ai = info.applicationInfo
                records += AppRecord(
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    userSerial = serial,
                    label = info.label?.toString().orEmpty().ifBlank { info.componentName.packageName },
                    isWork = isWork,
                    isDisabled = quiet || (ai.flags and ApplicationInfo.FLAG_SUSPENDED) != 0,
                    isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
        }
        publish(records)
        runCatching { snapshotFile.writeText(json.encodeToString(snapshotSerializer, records)) }
    }

    private fun publish(records: List<AppRecord>) {
        val list = records.map(::AppEntry)
        entries = list
        byId = list.associateBy { it.id }
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = scope.launch(io) {
            delay(300)
            reload()
        }
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach(::changed)
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach(::changed)
        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) =
            packageNames.forEach(::changed)
        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) =
            packageNames.forEach(::changed)

        private fun changed(packageName: String) {
            iconLoader.invalidate(packageName)
            scheduleReload()
        }
    }

    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = scheduleReload()
    }

    private companion object {
        const val PACKAGE_MATCH_PENALTY = -20
    }
}
