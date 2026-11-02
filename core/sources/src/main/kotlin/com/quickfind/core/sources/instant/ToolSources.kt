package com.quickfind.core.sources.instant

import android.content.Context
import android.text.format.DateFormat
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.instant.QueryDetectors
import com.quickfind.core.matching.instant.TimeCommands
import com.quickfind.core.matching.instant.UnitConverter
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultIcon
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.R
import com.quickfind.core.sources.SearchSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** Unit conversion, timers and alarms, computed from the query alone (spec 5.5). */
@Singleton
class ToolSource @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource {
    override val type = SourceType.TOOL
    override val kind = SourceKind.INSTANT
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.calculatorEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val raw = query.raw.trim()
        UnitConverter.parse(raw)?.let { c ->
            val result = UnitConverter.format(c.result)
            return listOf(
                tool(
                    id = "unit",
                    title = "$result ${c.to.symbol}",
                    subtitle = context.getString(R.string.unit_subtitle, UnitConverter.format(c.value), c.from.symbol),
                    action = ResultAction.CopyText(result),
                    symbol = "⇄",
                ),
            )
        }
        when (val cmd = TimeCommands.parse(raw)) {
            is TimeCommands.Command.Timer -> return listOf(
                tool(
                    id = "timer",
                    title = context.getString(R.string.timer_title, TimeCommands.describe(cmd.seconds)),
                    subtitle = context.getString(R.string.timer_subtitle),
                    action = ResultAction.SetTimer(cmd.seconds),
                    symbol = "⏱",
                ),
            )
            is TimeCommands.Command.Alarm -> return listOf(
                tool(
                    id = "alarm",
                    title = context.getString(
                        R.string.alarm_title,
                        TimeCommands.describeAlarm(cmd.hour, cmd.minute, DateFormat.is24HourFormat(context)),
                    ),
                    subtitle = context.getString(R.string.alarm_subtitle),
                    action = ResultAction.SetAlarm(cmd.hour, cmd.minute),
                    symbol = "⏰",
                ),
            )
            null -> Unit
        }
        return emptyList()
    }

    private fun tool(id: String, title: String, subtitle: String, action: ResultAction, symbol: String) = SearchResult(
        id = "tool:$id",
        type = type,
        title = title,
        subtitle = subtitle,
        matchScore = 1000,
        matchedRanges = emptyList(),
        primaryAction = action,
        icon = ResultIcon.Symbol(symbol),
    )
}

/**
 * Smart paste: an email address offers "Email", a postal address offers "Open in Maps".
 * Phone numbers and links are already handled by [PhoneNumberSource] and [UrlSource].
 */
@Singleton
class SmartPasteSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchSource {
    override val type = SourceType.TOOL
    override val kind = SourceKind.INSTANT
    override val isEnabled: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val raw = query.raw.trim()
        return when {
            QueryDetectors.looksLikeEmail(raw) -> listOf(
                SearchResult(
                    id = "tool:email:$raw",
                    type = type,
                    title = context.getString(R.string.email_title, raw),
                    subtitle = null,
                    matchScore = 140,
                    matchedRanges = emptyList(),
                    primaryAction = ResultAction.Email(raw),
                    secondaryActions = listOf(ResultAction.CopyText(raw)),
                    icon = ResultIcon.Symbol("@"),
                ),
            )
            QueryDetectors.looksLikeAddress(raw) -> listOf(
                SearchResult(
                    id = "tool:maps",
                    type = type,
                    title = context.getString(R.string.maps_title),
                    subtitle = raw,
                    matchScore = 140,
                    matchedRanges = emptyList(),
                    primaryAction = ResultAction.Navigate(raw),
                    secondaryActions = listOf(ResultAction.CopyText(raw)),
                    icon = ResultIcon.Symbol("⌖"),
                ),
            )
            else -> emptyList()
        }
    }
}
