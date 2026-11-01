package com.quickfind.core.sources.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.NormalizedText
import com.quickfind.core.matching.TextNormalizer
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.SearchSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private class SettingEntry(val shortcut: SettingShortcut, val title: String) {
    val normalizedTitle: NormalizedText = TextNormalizer.normalize(title)
    val normalizedKeywords: List<NormalizedText> = shortcut.keywords.map(TextNormalizer::normalize)
}

@Singleton
class SettingsSource @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SearchSource {
    override val type = SourceType.SETTING
    override val kind = SourceKind.IN_MEMORY
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.settingsEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    @Volatile private var entries: List<SettingEntry> = emptyList()

    override fun start() {
        scope.launch(io) { entries = resolve() }
    }

    /** Re-resolve when the screen opens, e.g. after a locale change or OS update. */
    override fun onVisible() {
        if (entries.isEmpty()) start()
    }

    private fun resolve(): List<SettingEntry> {
        val pm = context.packageManager
        val seenTitles = HashSet<String>()
        return SettingsCatalog.entries.mapNotNull { s ->
            if (Build.VERSION.SDK_INT < s.minSdk) return@mapNotNull null
            val resolves = runCatching {
                pm.resolveActivity(Intent(s.action), PackageManager.MATCH_DEFAULT_ONLY) != null
            }.getOrDefault(false)
            if (!resolves) return@mapNotNull null
            val title = context.getString(s.title)
            // Two actions may land on the same screen name (NFC panel and NFC settings).
            if (!seenTitles.add(title)) return@mapNotNull null
            SettingEntry(s, title)
        }
    }

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val out = ArrayList<Pair<Int, SearchResult>>()
        for (e in entries) {
            val titleMatch = FuzzyMatcher.match(query, e.normalizedTitle)
            var score = titleMatch?.score ?: Int.MIN_VALUE
            for (k in e.normalizedKeywords) {
                val m = FuzzyMatcher.match(query, k) ?: continue
                if (m.score + KEYWORD_PENALTY > score) score = m.score + KEYWORD_PENALTY
            }
            if (score == Int.MIN_VALUE) continue
            val ranges = if (titleMatch != null && titleMatch.score == score) titleMatch.ranges else emptyList()
            out += score to SearchResult(
                id = "setting:${e.shortcut.action}",
                type = type,
                title = e.title,
                subtitle = null,
                matchScore = score,
                matchedRanges = ranges,
                primaryAction = ResultAction.OpenSetting(e.shortcut.action),
            )
        }
        return out.sortedByDescending { it.first }.take(limit).map { it.second }
    }

    private companion object {
        const val KEYWORD_PENALTY = -5
    }
}
