package com.quickfind.core.sources.files

import android.content.Context
import android.text.format.Formatter
import com.quickfind.core.data.db.FileDao
import com.quickfind.core.data.db.FileEntity
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.FileNameTokens
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.TextNormalizer
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultIcon
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.R
import com.quickfind.core.sources.SearchSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Files and folders from the Room index (spec 5.3). Indexed source: FTS4 prefix MATCH finds
 * up to [CANDIDATES] rows, then the fuzzy matcher re-scores them on the display name.
 */
@Singleton
class FileSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FileDao,
    private val settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource {
    override val type = SourceType.FILE
    override val kind = SourceKind.INDEXED
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.filesEnabled }.stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val match = FileNameTokens.ftsMatch(query) ?: return emptyList()
        val candidates = LinkedHashMap<Long, FileEntity>()
        dao.ftsSearch(match, CANDIDATES).forEach { candidates[it.id] = it }
        if (candidates.size < limit) {
            // Mid-word fragments ("sume" in "resume") are invisible to FTS prefix queries.
            val longest = query.tokens.maxByOrNull { it.length }
            if (longest != null && longest.length >= 3) {
                dao.substringSearch(longest, CANDIDATES / 2).forEach { candidates.putIfAbsent(it.id, it) }
            }
        }
        val excluded = settings.settings.value.excludedFolders
        val scored = ArrayList<Pair<Int, SearchResult>>(candidates.size)
        for ((i, f) in candidates.values.withIndex()) {
            if (i and 31 == 0) currentCoroutineContext().ensureActive()
            val fullPath = if (f.isFolder) f.contentUri else f.relativePath
            if (excluded.any { fullPath.startsWith(it) }) continue
            val m = FuzzyMatcher.match(query, TextNormalizer.normalize(f.displayName))
            // Matched only through the folder path: keep, but well below name matches.
            val score = m?.score ?: PATH_ONLY_SCORE
            scored += score to toResult(f, score, m?.ranges.orEmpty())
        }
        return scored.sortedByDescending { it.first }.take(limit).map { it.second }
    }

    override suspend fun recent(limit: Int): List<SearchResult> =
        if (!isEnabled.value) emptyList() else dao.recentFiles(limit).map { toResult(it, 0, emptyList()) }

    /** Lists one folder from the index for the in-app folder view. */
    suspend fun folderContents(relativePath: String): List<SearchResult> =
        dao.inFolder(relativePath).map { toResult(it, 0, emptyList()) }

    fun toResult(f: FileEntity, score: Int, ranges: List<IntRange>): SearchResult {
        if (f.isFolder) {
            return SearchResult(
                id = "folder:${f.contentUri}",
                type = SourceType.FOLDER,
                title = f.displayName,
                subtitle = f.relativePath.ifEmpty { context.getString(R.string.storage_root) },
                matchScore = score,
                matchedRanges = ranges,
                primaryAction = ResultAction.OpenFolder(f.contentUri, f.displayName),
            )
        }
        val size = Formatter.formatShortFileSize(context, f.size)
        val parentName = f.relativePath.trimEnd('/').substringAfterLast('/').ifEmpty { context.getString(R.string.storage_root) }
        val isVisualMedia = f.mimeType?.startsWith("image/") == true || f.mimeType?.startsWith("video/") == true
        return SearchResult(
            id = "file:${f.sourceKey}",
            type = SourceType.FILE,
            title = f.displayName,
            subtitle = "${f.relativePath.ifEmpty { "/" }} · $size",
            matchScore = score,
            matchedRanges = ranges,
            primaryAction = ResultAction.OpenFile(f.contentUri, f.mimeType),
            secondaryActions = listOf(
                ResultAction.ShareFile(f.contentUri, f.mimeType),
                ResultAction.OpenFolder(f.relativePath, parentName),
            ),
            icon = if (isVisualMedia) ResultIcon.FileThumbnail(f.contentUri, f.mimeType) else ResultIcon.Glyph(SourceType.FILE),
        )
    }

    private companion object {
        /** Spec 4.3: re-score the top ~200 FTS hits. */
        const val CANDIDATES = 200
        const val PATH_ONLY_SCORE = 5
    }
}
