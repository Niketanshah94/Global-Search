package com.quickfind.core.sources.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.SearchableText
import com.quickfind.core.matching.match
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.SearchSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** Calendar events from 30 days ago to 30 days ahead (spec 10.8). Needs READ_CALENDAR. */
@Singleton
class CalendarSource @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource {
    override val type = SourceType.CALENDAR
    override val kind = SourceKind.INDEXED
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.calendarEnabled }.stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val token = query.tokens.maxByOrNull { it.length } ?: return emptyList()
        if (token.length < 2) return emptyList()
        val now = System.currentTimeMillis()
        // The provider's own search covers title, description and location.
        val uri = Uri.withAppendedPath(
            CalendarContract.Instances.CONTENT_SEARCH_URI,
            "${now - WINDOW_MS}/${now + WINDOW_MS}/${Uri.encode(token)}",
        )
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
        )
        val out = ArrayList<Pair<Int, SearchResult>>()
        context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext() && out.size < CANDIDATES) {
                val eventId = c.getLong(0)
                val title = c.getString(1).orEmpty()
                if (title.isBlank()) continue
                val begin = c.getLong(2)
                val end = c.getLong(3)
                val allDay = c.getInt(4) == 1
                val location = c.getString(5)?.takeIf { it.isNotBlank() }
                val m = FuzzyMatcher.match(query, SearchableText(title))
                // Upcoming events matter more than past ones; today's most of all.
                val days = (begin - now) / DateUtils.DAY_IN_MILLIS
                val recency = when {
                    days in 0..1 -> 30
                    days > 1 -> (20 - days.toInt() / 2).coerceAtLeast(0)
                    else -> (10 - abs(days.toInt())).coerceAtLeast(0)
                }
                val score = (m?.score ?: DETAILS_MATCH_SCORE) + recency
                var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
                if (!allDay) flags = flags or DateUtils.FORMAT_SHOW_TIME
                val whenText = DateUtils.formatDateRange(context, begin, end, flags)
                out += score to SearchResult(
                    id = "calendar:$eventId:$begin",
                    type = type,
                    title = title,
                    subtitle = listOfNotNull(whenText, location).joinToString(" · "),
                    matchScore = score,
                    matchedRanges = m?.ranges.orEmpty(),
                    primaryAction = ResultAction.OpenCalendarEvent(eventId, begin, end),
                    secondaryActions = listOfNotNull(location?.let { ResultAction.Navigate(it) }),
                )
            }
        }
        return out.sortedByDescending { it.first }.take(limit).map { it.second }
    }

    private companion object {
        const val WINDOW_MS = 30L * 24 * 60 * 60 * 1000
        const val CANDIDATES = 100
        const val DETAILS_MATCH_SCORE = 15
    }
}
