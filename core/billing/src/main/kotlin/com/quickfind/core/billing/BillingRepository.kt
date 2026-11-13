package com.quickfind.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.quickfind.core.data.billing.EntitlementRepository
import com.quickfind.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

enum class BillingAvailability {
    CONNECTING,
    READY,

    /** No Google Play (sideloaded or test build, no Play account, or Play too old). */
    UNAVAILABLE,
}

sealed interface PurchaseEvent {
    data object Subscribed : PurchaseEvent
    data object Pending : PurchaseEvent
    data object Cancelled : PurchaseEvent
    data class Failed(val code: Int) : PurchaseEvent
}

/**
 * FindPal Pro through Google Play Billing: one subscription ("findpal_pro") with a monthly
 * and a yearly base plan, each with a 7-day free-trial offer for new subscribers.
 *
 * No server: Play's purchase list (signature-checked when a licence key is configured) is the
 * source of truth, cached in [EntitlementRepository] so Pro works offline. Purchases are
 * acknowledged here; Play refunds any subscription not acknowledged within 3 days.
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entitlements: EntitlementRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val _availability = MutableStateFlow(BillingAvailability.CONNECTING)
    val availability: StateFlow<BillingAvailability> = _availability.asStateFlow()

    private val _plans = MutableStateFlow<List<PlanOption>>(emptyList())
    val plans: StateFlow<List<PlanOption>> = _plans.asStateFlow()

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    @Volatile private var productDetails: ProductDetails? = null
    private val connecting = AtomicBoolean(false)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    /** Play's page where the user can cancel or change the plan. */
    val manageSubscriptionUrl: String
        get() = "https://play.google.com/store/account/subscriptions?sku=${PlanCatalog.PRODUCT_ID}&package=${context.packageName}"

    /** Connects once at process start; [refresh] reconnects after Play drops the connection. */
    fun start() = connect()

    /** Re-reads prices and the user's subscription (app to foreground, paywall opened, "Restore"). */
    fun refresh() {
        if (client.isReady) {
            scope.launch {
                loadPlans()
                syncPurchases()
            }
        } else {
            connect()
        }
    }

    /** Opens Google Play's purchase sheet. Returns false when it could not be shown. */
    fun purchase(activity: Activity, plan: PlanOption): Boolean {
        val details = productDetails ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(plan.offerToken)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            _events.tryEmit(PurchaseEvent.Failed(result.responseCode))
            return false
        }
        return true
    }

    private fun connect() {
        if (client.isReady || !connecting.compareAndSet(false, true)) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting.set(false)
                if (result.responseCode == BillingResponseCode.OK) {
                    _availability.value = BillingAvailability.READY
                    scope.launch {
                        loadPlans()
                        syncPurchases()
                    }
                } else {
                    // Keep the cached entitlement: no Play is not proof the subscription ended.
                    _availability.value = BillingAvailability.UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting.set(false)
                _availability.value = BillingAvailability.CONNECTING
            }
        })
    }

    private suspend fun loadPlans() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PlanCatalog.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingResponseCode.OK) return
        val details = result.productDetailsList.orEmpty().firstOrNull { it.productId == PlanCatalog.PRODUCT_ID } ?: return
        productDetails = details
        _plans.value = PlanCatalog.plans(
            details.subscriptionOfferDetails.orEmpty().map { offer ->
                OfferInfo(
                    basePlanId = offer.basePlanId,
                    offerId = offer.offerId,
                    offerToken = offer.offerToken,
                    phases = offer.pricingPhases.pricingPhaseList.map { p ->
                        PhaseInfo(p.priceAmountMicros, p.formattedPrice, p.priceCurrencyCode, p.billingPeriod, p.billingCycleCount)
                    },
                )
            },
        )
    }

    /** Play lists active subscriptions only (trial and grace period included, not on hold or expired). */
    private suspend fun syncPurchases() {
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) return
        val active = acceptActive(result.purchasesList)
        entitlements.setSubscribed(active)
    }

    /** Acknowledges new purchases; true when at least one valid FindPal Pro purchase is active. */
    private suspend fun acceptActive(purchases: List<Purchase>): Boolean {
        val valid = purchases.filter { p ->
            PlanCatalog.PRODUCT_ID in p.products &&
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                PurchaseVerifier.isValid(BuildConfig.PLAY_LICENSE_KEY, p.originalJson, p.signature)
        }
        for (p in valid) {
            if (!p.isAcknowledged) {
                // A failure here is retried on the next refresh, well inside Play's 3 days.
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
            }
        }
        return valid.isNotEmpty()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch {
                val list = purchases.orEmpty()
                if (acceptActive(list)) {
                    entitlements.setSubscribed(true)
                    _events.emit(PurchaseEvent.Subscribed)
                } else if (list.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    _events.emit(PurchaseEvent.Pending)
                } else {
                    _events.emit(PurchaseEvent.Failed(result.responseCode))
                }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch {
                syncPurchases()
                _events.emit(PurchaseEvent.Subscribed)
            }
            BillingResponseCode.USER_CANCELED -> _events.tryEmit(PurchaseEvent.Cancelled)
            else -> _events.tryEmit(PurchaseEvent.Failed(result.responseCode))
        }
    }
}
