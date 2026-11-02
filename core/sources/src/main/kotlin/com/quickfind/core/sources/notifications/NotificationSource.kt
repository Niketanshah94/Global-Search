package com.quickfind.core.sources.notifications

import android.text.format.DateUtils
import com.quickfind.core.data.db.NotificationDao
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.OtpDetector
import com.quickfind.core.matching.SearchableText
import com.quickfind.core.matching.match
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.OtpProvider
import com.quickfind.core.sources.SearchSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSource @Inject constructor(
    private val dao: NotificationDao,
    private val store: NotificationStore,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource, OtpProvider {
    override val type = SourceType.NOTIFICATION
    override val kind = SourceKind.INDEXED
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.notificationsEnabled }.stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val token = query.tokens.maxByOrNull { it.length } ?: return emptyList()
        if (token.length < 2) return emptyList()
        val now = System.currentTimeMillis()
        val rows = dao.search("%${token.replace("%", "").replace("_", "")}%", now - NotificationStore.RETENTION_MS, CANDIDATES)
        return rows.map { n ->
            val m = FuzzyMatcher.match(query, SearchableText(n.title))
            val score = (m?.score ?: TEXT_MATCH_SCORE) - ((now - n.postedAt) / DateUtils.HOUR_IN_MILLIS).toInt().coerceAtMost(30)
            val otp = OtpDetector.find("${n.title} ${n.text}")
            val ago = DateUtils.getRelativeTimeSpanString(n.postedAt, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
            SearchResult(
                id = "notification:${n.key}",
                type = type,
                title = n.title.ifBlank { n.appLabel },
                subtitle = "${n.appLabel} · $ago · ${n.text}".take(160),
                matchScore = score,
                matchedRanges = if (n.title.isBlank()) emptyList() else m?.ranges.orEmpty(),
                primaryAction = ResultAction.OpenNotification(n.sbnKey, n.packageName),
                secondaryActions = listOfNotNull(otp?.let { ResultAction.CopyText(it) }, ResultAction.CopyText(n.text)),
            ) to score
        }.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    /** A code that arrived in the last 10 minutes, for the empty-screen chip. */
    override fun latestOtp(): OtpProvider.Code? {
        if (!isEnabled.value) return null
        val otp = store.latestOtp ?: return null
        if (System.currentTimeMillis() - otp.postedAt > OTP_FRESH_MS) return null
        return OtpProvider.Code(otp.code, otp.source)
    }

    private companion object {
        const val CANDIDATES = 100
        const val TEXT_MATCH_SCORE = 20
        const val OTP_FRESH_MS = 10L * 60 * 1000
    }
}
