package com.quickfind.core.sources.instant

import android.content.Context
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.PhoneNumbers
import com.quickfind.core.matching.instant.Calculator
import com.quickfind.core.matching.instant.QueryDetectors
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.R
import com.quickfind.core.sources.SearchSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalculatorSource @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource {
    override val type = SourceType.CALCULATOR
    override val kind = SourceKind.INSTANT
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.calculatorEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val value = Calculator.evaluate(query.raw) ?: return emptyList()
        val text = Calculator.format(value)
        return listOf(
            SearchResult(
                id = "calc",
                type = type,
                title = text,
                subtitle = context.getString(R.string.calculator_subtitle, query.raw.trim()),
                matchScore = 1000,
                matchedRanges = emptyList(),
                primaryAction = ResultAction.CopyText(text),
            ),
        )
    }
}

@Singleton
class PhoneNumberSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedApps: InstalledApps,
) : SearchSource {
    override val type = SourceType.PHONE_NUMBER
    override val kind = SourceKind.INSTANT
    override val isEnabled: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        if (!QueryDetectors.looksLikePhoneNumber(query.raw)) return emptyList()
        val number = query.raw.trim()
        val secondary = buildList {
            add(ResultAction.Sms(number))
            if (installedApps.whatsAppPackage != null) add(ResultAction.WhatsApp(number))
            add(ResultAction.CopyText(number))
        }
        return listOf(
            SearchResult(
                id = "phone:${PhoneNumbers.digitsOnly(number)}",
                type = type,
                title = number,
                subtitle = context.getString(R.string.phone_subtitle),
                matchScore = 150,
                matchedRanges = emptyList(),
                primaryAction = ResultAction.Call(number),
                secondaryActions = secondary,
            ),
        )
    }
}

@Singleton
class UrlSource @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource {
    override val type = SourceType.URL
    override val kind = SourceKind.INSTANT
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.webEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        if (!QueryDetectors.looksLikeUrl(query.raw)) return emptyList()
        val url = QueryDetectors.toUrl(query.raw)
        return listOf(
            SearchResult(
                id = "url:$url",
                type = type,
                title = context.getString(R.string.url_title, query.raw.trim()),
                subtitle = url,
                matchScore = 120,
                matchedRanges = emptyList(),
                primaryAction = ResultAction.OpenUrl(url),
                secondaryActions = listOf(ResultAction.CopyText(url)),
            ),
        )
    }
}

/** "Search Google for '…'", always the last item (spec 5.5). */
@Singleton
class WebSearchSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource {
    override val type = SourceType.WEB
    override val kind = SourceKind.INSTANT
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.webEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val text = query.raw.trim()
        if (text.isEmpty()) return emptyList()
        val s = settings.settings.value
        val engine = query.webEngine ?: s.searchEngine
        val url = QueryDetectors.webSearchUrl(engine, text, s.customSearchTemplate)
        val engineName = if (engine == com.quickfind.core.model.WebSearchEngine.CUSTOM) {
            context.getString(R.string.web_engine_custom)
        } else {
            engine.displayName
        }
        return listOf(
            SearchResult(
                id = "web",
                type = type,
                title = context.getString(R.string.web_search_title, engineName, text),
                subtitle = null,
                // A "yt " prefix means the user wants this result first.
                matchScore = if (query.webEngine != null) 2000 else 0,
                matchedRanges = emptyList(),
                primaryAction = ResultAction.OpenUrl(url),
            ),
        )
    }
}
