package com.quickfind.core.sources.instant

import android.content.Context
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.instant.CurrencyQuery
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultIcon
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.R
import com.quickfind.core.sources.SearchSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RatesResponse(val base: String, val date: String, val rates: Map<String, Double>)

@Serializable
private data class CachedRates(val fetchedAt: Long, val date: String, val perEur: Map<String, Double>)

/**
 * European Central Bank reference rates via the Frankfurter API (no key, no account).
 * The request carries no user data: only "give me today's rates". Cached for 12 hours.
 */
@Singleton
class CurrencyRates @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val file = File(context.cacheDir, "currency_rates.json")
    private var cached: CachedRates? = null

    /** Rates per 1 EUR, plus the ECB date; null when offline with nothing cached. */
    suspend fun rates(): Pair<Map<String, Double>, String>? = mutex.withLock {
        withContext(io) {
            val now = System.currentTimeMillis()
            val current = cached ?: runCatching { json.decodeFromString(CachedRates.serializer(), file.readText()) }.getOrNull()
            if (current != null && now - current.fetchedAt < MAX_AGE_MS) {
                cached = current
                return@withContext current.perEur to current.date
            }
            val fresh = runCatching { download() }.getOrNull()
            if (fresh != null) {
                val entry = CachedRates(now, fresh.date, fresh.rates + ("EUR" to 1.0))
                cached = entry
                runCatching { file.writeText(json.encodeToString(CachedRates.serializer(), entry)) }
                entry.perEur to entry.date
            } else {
                // Offline: stale rates are better than nothing; the date is shown to the user.
                current?.let { it.perEur to it.date }
            }
        }
    }

    private fun download(): RatesResponse {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000
        connection.setRequestProperty("Accept", "application/json")
        return try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(RatesResponse.serializer(), body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.frankfurter.app/latest"
        const val MAX_AGE_MS = 12L * 60 * 60 * 1000
    }
}

/** "100 usd in inr", "$50" (to the phone's local currency), "€20 to inr". */
@Singleton
class CurrencySource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rates: CurrencyRates,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SearchSource {
    override val type = SourceType.TOOL
    // Indexed, not instant: the first query may wait on the network, and it should not block typing.
    override val kind = SourceKind.INDEXED
    override val isEnabled: StateFlow<Boolean> =
        settings.settings.map { it.currencyEnabled }.stateIn(scope, SharingStarted.Eagerly, true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val request = CurrencyQuery.parse(query.raw) ?: return emptyList()
        val local = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
        val to = request.to ?: (if (local != null && local != request.from && local in CurrencyQuery.SUPPORTED) local else if (request.from == "USD") "EUR" else "USD")
        val (perEur, date) = rates.rates() ?: return emptyList()
        val fromRate = perEur[request.from] ?: return emptyList()
        val toRate = perEur[to] ?: return emptyList()
        val converted = request.amount / fromRate * toRate
        val text = CurrencyQuery.format(converted, to)
        return listOf(
            SearchResult(
                id = "tool:currency",
                type = type,
                title = text,
                subtitle = context.getString(R.string.currency_subtitle, CurrencyQuery.format(request.amount, request.from), date),
                matchScore = 1000,
                matchedRanges = emptyList(),
                primaryAction = ResultAction.CopyText(String.format(Locale.US, "%.2f", converted)),
                icon = ResultIcon.Symbol(CurrencyQuery.symbolOrCode(to)),
            ),
        )
    }
}
