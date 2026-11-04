package com.quickfind.core.sources.engine

import com.quickfind.core.data.di.DefaultDispatcher
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.data.history.HistoryRepository
import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.ranking.AssembledResults
import com.quickfind.core.matching.instant.QueryPrefixes
import com.quickfind.core.matching.ranking.ResultAssembler
import com.quickfind.core.model.WebSearchEngine
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.OtpProvider
import com.quickfind.core.sources.SearchSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SearchRequest(
    val text: String,
    /** When set, only these source types run (static shortcuts "Search files"/"Search contacts"). */
    val scope: Set<SourceType>? = null,
)

data class EngineResult(
    val query: String,
    val results: AssembledResults,
    /** False while indexed sources are still running; their results merge in later. */
    val isComplete: Boolean,
)

data class EmptyStateContent(
    val pinned: List<SearchResult>,
    val recentApps: List<SearchResult>,
    val recentFiles: List<SearchResult>,
    val otp: OtpProvider.Code? = null,
    /** Results opened since local midnight. */
    val foundToday: Int = 0,
)

/**
 * The query pipeline (spec 4.4). UI-agnostic so the standalone screen and the future
 * launcher share it.
 *
 * Every new query cancels the previous one (transformLatest). In-memory and instant sources
 * run immediately and are emitted first; indexed sources wait [INDEXED_DEBOUNCE_MS] (cancelled
 * if the user keeps typing) and merge in when ready.
 */
@Singleton
class SearchEngine @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards SearchSource>,
    private val history: HistoryRepository,
    private val resultPrefs: ResultPrefsRepository,
    private val settings: SettingsRepository,
    @DefaultDispatcher private val cpu: CoroutineDispatcher,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        sources.forEach { it.start() }
    }

    fun onVisible() = sources.forEach { it.onVisible() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun results(requests: Flow<SearchRequest>): Flow<EngineResult> = requests.transformLatest { request ->
        val user = settings.settings.value
        // Prefix shortcuts: "f report" (files only), "yt lofi" (YouTube search).
        var text = request.text
        var scopeTypes = request.scope
        var forcedEngine: WebSearchEngine? = null
        if (user.prefixesEnabled) {
            QueryPrefixes.parse(text)?.let { parsed ->
                when (val prefix = parsed.prefix) {
                    is QueryPrefixes.Prefix.Scope -> if (scopeTypes == null) {
                        scopeTypes = prefix.types
                        text = parsed.rest
                    }
                    is QueryPrefixes.Prefix.Web -> {
                        forcedEngine = prefix.engine
                        text = parsed.rest
                    }
                }
                Unit
            }
        }
        val query = NormalizedQuery.of(text).withWebEngine(forcedEngine)
        if (query.isEmpty) {
            emit(EngineResult(request.text, AssembledResults.EMPTY, isComplete = true))
            return@transformLatest
        }
        val scope = scopeTypes
        val active = if (forcedEngine != null) {
            sources.filter { it.type == SourceType.WEB }
        } else {
            sources.filter { s ->
                s.isEnabled.value && (scope == null || s.coversTypes.any { it in scope } || s.type == SourceType.WEB)
            }
        }
        val inScope: (SearchResult) -> Boolean = { r -> scope == null || r.type in scope || r.type == SourceType.WEB }
        val context = history.rankingContext(query.normalized, user.rankingMode, user.typeBias)
        val hidden = resultPrefs.hiddenIds.value

        coroutineScope {
            val indexed = active.filter { it.kind == SourceKind.INDEXED }
            val indexedJobs = indexed.map { source ->
                async(io) {
                    delay(INDEXED_DEBOUNCE_MS)
                    safeSearch(source, query)
                }
            }
            val fast = active.filter { it.kind != SourceKind.INDEXED }
                .map { source -> async(cpu) { safeSearch(source, query) } }
                .awaitAll()
                .flatten()
                .filter(inScope)

            val fastAssembled = withContext(cpu) { ResultAssembler.assemble(fast, context, hidden) }
            emit(EngineResult(request.text, fastAssembled, isComplete = indexed.isEmpty()))

            if (indexed.isNotEmpty()) {
                val all = fast + indexedJobs.awaitAll().flatten().filter(inScope)
                val assembled = withContext(cpu) { ResultAssembler.assemble(all, context, hidden) }
                emit(EngineResult(request.text, assembled, isComplete = true))
            }
        }
    }

    suspend fun emptyState(): EmptyStateContent {
        val hidden = resultPrefs.hiddenIds.value
        val enabled = sources.filter { it.isEnabled.value }
        val recentIds = history.topLaunchedIds(RECENT_APPS * 2)
        val recentApps = recentIds.asSequence()
            .filter { it.startsWith("app:") && it !in hidden }
            .mapNotNull { id -> enabled.firstNotNullOfOrNull { it.resultForId(id) } }
            .take(RECENT_APPS)
            .toList()
        val recentFiles = enabled.flatMap { runCatching { it.recent(RECENT_FILES) }.getOrDefault(emptyList()) }
            .filter { it.id !in hidden }
        val otp = sources.filterIsInstance<OtpProvider>().firstNotNullOfOrNull { it.latestOtp() }
        val midnight = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return EmptyStateContent(
            pinned = emptyList(),
            recentApps = recentApps,
            recentFiles = recentFiles,
            otp = otp,
            foundToday = history.launchesSince(midnight),
        )
    }

    /** Learns from the user's pick (spec 4.7 historyBoost/frecency). */
    suspend fun recordLaunch(rawQuery: String, result: SearchResult) {
        // Instant results are recomputed from the query; learning them would only add noise.
        if (result.type in setOf(SourceType.CALCULATOR, SourceType.TOOL, SourceType.WEB, SourceType.URL, SourceType.PHONE_NUMBER)) return
        history.recordLaunch(NormalizedQuery.of(rawQuery).normalized, result.id)
    }

    private suspend fun safeSearch(source: SearchSource, query: NormalizedQuery): List<SearchResult> = try {
        source.search(query, PER_SOURCE_LIMIT)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // A failing source (revoked permission, OEM provider bug) must never break search.
        // Deliberately not logging: the exception message could include query text.
        emptyList()
    }

    companion object {
        const val INDEXED_DEBOUNCE_MS = 80L
        const val PER_SOURCE_LIMIT = 20
        const val RECENT_APPS = 8
        const val RECENT_FILES = 6
    }
}
