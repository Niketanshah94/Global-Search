package com.quickfind.core.data.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.quickfind.core.data.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class Entitlement(
    /** Google Play reports an active FindPal Pro subscription (the free trial counts). */
    val subscribed: Boolean = false,
    /** Test builds only: Pro unlocked by hand from Settings, for trying Pro without Play. */
    val testUnlock: Boolean = false,
    /** False until the stored values have loaded. */
    val loaded: Boolean = false,
) {
    val isPro: Boolean get() = subscribed || testUnlock
}

/**
 * The last known subscription state, stored on the phone so Pro keeps working offline and
 * on a cold start before Google Play answers. Billing updates it whenever Play reports.
 */
interface EntitlementRepository {
    val entitlement: StateFlow<Entitlement>

    val isPro: Boolean get() = entitlement.value.isPro

    /** Waits for the stored state (instant after the first load). */
    suspend fun current(): Entitlement = entitlement.first { it.loaded }

    suspend fun setSubscribed(active: Boolean)

    suspend fun setTestUnlock(on: Boolean)
}

@Singleton
class DataStoreEntitlementRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope,
) : EntitlementRepository {
    override val entitlement: StateFlow<Entitlement> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p -> Entitlement(subscribed = p[SUBSCRIBED] ?: false, testUnlock = p[TEST_UNLOCK] ?: false, loaded = true) }
        .stateIn(scope, SharingStarted.Eagerly, Entitlement())

    override suspend fun setSubscribed(active: Boolean) {
        dataStore.edit { it[SUBSCRIBED] = active }
    }

    override suspend fun setTestUnlock(on: Boolean) {
        dataStore.edit { it[TEST_UNLOCK] = on }
    }

    private companion object {
        val SUBSCRIBED = booleanPreferencesKey("pro_subscribed")
        val TEST_UNLOCK = booleanPreferencesKey("pro_test_unlock")
    }
}
