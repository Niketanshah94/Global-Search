package com.quickfind.core.sources.contacts

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import androidx.core.content.ContextCompat
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.NormalizedText
import com.quickfind.core.matching.SearchableText
import com.quickfind.core.matching.match
import com.quickfind.core.matching.PhoneNumbers
import com.quickfind.core.matching.TextNormalizer
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultIcon
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.SearchSource
import com.quickfind.core.sources.instant.InstalledApps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private class ContactEntry(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val photoUri: String?,
    val phones: List<String>,
    val emails: List<String>,
    val nicknames: List<String>,
    val addresses: List<String>,
) {
    // Searchable in its own script and romanized: "rahul" finds "राहुल".
    val normalizedName = SearchableText(name)
    val normalizedNicknames: List<SearchableText> = nicknames.map(::SearchableText)
    val normalizedEmails: List<NormalizedText> = emails.map { TextNormalizer.normalize(it.substringBefore('@')) }
    val phoneDigits: List<String> = phones.map(PhoneNumbers::digitsOnly)
    val initials: String = name.split(' ').filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
}

/**
 * Contacts by name, nickname, email and partial number (spec 5.2). Needs READ_CONTACTS;
 * stays empty (and asks nothing) until the user enables the source.
 */
@Singleton
class ContactSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val installedApps: InstalledApps,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SearchSource {
    override val type = SourceType.CONTACT
    override val kind = SourceKind.IN_MEMORY
    override val isEnabled: StateFlow<Boolean> = settings.settings
        .map { it.contactsEnabled }
        .stateIn(scope, SharingStarted.Eagerly, false)

    @Volatile private var entries: List<ContactEntry> = emptyList()
    private var observerRegistered = false
    private var reloadJob: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleReload(OBSERVER_DEBOUNCE_MS)
    }

    override fun start() {
        scope.launch {
            settings.loaded.map { it.contactsEnabled }.distinctUntilChanged().collect { enabled ->
                if (enabled) refreshIfPermitted() else disable()
            }
        }
    }

    /** Permission may have just been granted from the settings screen. */
    override fun onVisible() {
        if (isEnabled.value && entries.isEmpty()) refreshIfPermitted()
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun refreshIfPermitted() {
        if (!hasPermission()) return
        if (!observerRegistered) {
            context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            observerRegistered = true
        }
        scheduleReload(0)
    }

    private fun disable() {
        reloadJob?.cancel()
        entries = emptyList()
        if (observerRegistered) {
            context.contentResolver.unregisterContentObserver(observer)
            observerRegistered = false
        }
    }

    private fun scheduleReload(delayMs: Long) {
        reloadJob?.cancel()
        reloadJob = scope.launch(io) {
            delay(delayMs)
            if (hasPermission()) entries = load()
        }
    }

    private fun load(): List<ContactEntry> {
        class Builder(val id: Long, val lookup: String, val name: String, val photo: String?) {
            val phones = LinkedHashMap<String, String>()
            val emails = LinkedHashSet<String>()
            val nicknames = LinkedHashSet<String>()
            val addresses = LinkedHashSet<String>()
        }
        val builders = LinkedHashMap<Long, Builder>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
        )
        val types = arrayOf(
            CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
            CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
        )
        val selection = "${ContactsContract.Data.MIMETYPE} IN (?,?,?,?,?)"
        context.contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, types, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val iLookup = c.getColumnIndexOrThrow(ContactsContract.Data.LOOKUP_KEY)
            val iName = c.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
            val iPhoto = c.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_THUMBNAIL_URI)
            val iMime = c.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val iData = c.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val name = c.getString(iName) ?: continue
                val b = builders.getOrPut(id) {
                    Builder(id, c.getString(iLookup).orEmpty(), name, c.getString(iPhoto))
                }
                val data = c.getString(iData)?.trim().orEmpty()
                if (data.isEmpty()) continue
                when (c.getString(iMime)) {
                    // Dedupe "+91 98765 43210" and "098765 43210" style duplicates by digits.
                    CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> b.phones.putIfAbsent(PhoneNumbers.digitsOnly(data).takeLast(10), data)
                    CommonDataKinds.Email.CONTENT_ITEM_TYPE -> b.emails += data
                    CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> b.nicknames += data
                    // DATA1 of a postal row is the formatted address.
                    CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> b.addresses += data.replace('\n', ' ')
                }
            }
        }
        return builders.values.map {
            ContactEntry(
                it.id, it.lookup, it.name, it.photo,
                it.phones.values.toList(), it.emails.toList(), it.nicknames.toList(), it.addresses.toList(),
            )
        }
    }

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val snapshot = entries
        if (snapshot.isEmpty()) return emptyList()
        val apps = MessagingApps(
            whatsApp = installedApps.whatsAppPackage != null,
            telegram = installedApps.hasTelegram,
            signal = installedApps.hasSignal,
        )
        val out = ArrayList<Pair<Int, SearchResult>>()
        for ((i, e) in snapshot.withIndex()) {
            if ((i and 63) == 0) currentCoroutineContext().ensureActive()
            var score = Int.MIN_VALUE
            var ranges = emptyList<IntRange>()
            var matchedPhone: String? = null

            FuzzyMatcher.match(query, e.normalizedName)?.let {
                score = it.score
                ranges = it.ranges
            }
            for (nick in e.normalizedNicknames) {
                FuzzyMatcher.match(query, nick)?.let { if (it.score + NICKNAME_PENALTY > score) { score = it.score + NICKNAME_PENALTY; ranges = emptyList() } }
            }
            for (email in e.normalizedEmails) {
                FuzzyMatcher.match(query, email)?.let { if (it.score + EMAIL_PENALTY > score) { score = it.score + EMAIL_PENALTY; ranges = emptyList() } }
            }
            if (query.looksNumeric) {
                for ((idx, digits) in e.phoneDigits.withIndex()) {
                    val s = PhoneNumbers.matchScore(query.digits, digits) ?: continue
                    if (s > score) {
                        score = s
                        ranges = emptyList()
                        matchedPhone = e.phones[idx]
                    }
                }
            }
            if (score == Int.MIN_VALUE) continue
            out += score to e.toResult(score, ranges, matchedPhone, apps)
        }
        return out.sortedByDescending { it.first }.take(limit).map { it.second }
    }

    private data class MessagingApps(val whatsApp: Boolean, val telegram: Boolean, val signal: Boolean)

    private fun ContactEntry.toResult(score: Int, ranges: List<IntRange>, matchedPhone: String?, apps: MessagingApps): SearchResult {
        val phone = matchedPhone ?: phones.firstOrNull()
        val email = emails.firstOrNull()
        val contactUri = ContactsContract.Contacts.getLookupUri(id, lookupKey)
            ?: ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
        val open = ResultAction.OpenContact(contactUri.toString())
        val primary: ResultAction = when {
            phone != null -> ResultAction.Call(phone)
            email != null -> ResultAction.Email(email)
            else -> open
        }
        val secondary = buildList {
            if (phone != null) {
                add(ResultAction.Sms(phone))
                if (apps.whatsApp) add(ResultAction.WhatsApp(phone))
                if (apps.telegram) add(ResultAction.Telegram(phone))
                if (apps.signal) add(ResultAction.Signal(phone))
            }
            addresses.firstOrNull()?.let { add(ResultAction.Navigate(it)) }
            if (email != null && primary !is ResultAction.Email) add(ResultAction.Email(email))
            if (primary != open) add(open)
            // Extra numbers, so a contact with "Mobile" and "Work" can call either.
            phones.filter { it != phone }.take(2).forEach { add(ResultAction.Call(it)) }
        }
        return SearchResult(
            id = "contact:$id",
            type = SourceType.CONTACT,
            title = name,
            subtitle = phone ?: email,
            matchScore = score,
            matchedRanges = ranges,
            primaryAction = primary,
            secondaryActions = secondary,
            icon = ResultIcon.ContactPhoto(photoUri, initials),
        )
    }

    private companion object {
        const val OBSERVER_DEBOUNCE_MS = 2_000L
        const val NICKNAME_PENALTY = -10
        const val EMAIL_PENALTY = -15
    }
}

