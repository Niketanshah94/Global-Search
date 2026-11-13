package com.quickfind.core.billing

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.math.roundToInt

enum class PlanPeriod { MONTHLY, YEARLY }

/** One plan the paywall can sell: a base plan plus the best offer the user is eligible for. */
data class PlanOption(
    val basePlanId: String,
    val period: PlanPeriod,
    /** Recurring price as Play formats it for the user's country, e.g. "$4.99" or "₹149.00". */
    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    /** 0 when the user already had a trial (Play then offers no trial offer). */
    val freeTrialDays: Int,
    val offerToken: String,
) {
    val hasFreeTrial: Boolean get() = freeTrialDays > 0
}

/** Plain copies of Play's offer data, so the plan choice is testable without Play. */
data class OfferInfo(val basePlanId: String, val offerId: String?, val offerToken: String, val phases: List<PhaseInfo>)

data class PhaseInfo(val priceMicros: Long, val formattedPrice: String, val currencyCode: String, val billingPeriod: String, val cycles: Int)

object PlanCatalog {
    /** Play Console product and base plan ids. They must match docs/store/BILLING.md. */
    const val PRODUCT_ID = "findpal_pro"
    const val MONTHLY = "monthly"
    const val YEARLY = "yearly"

    /**
     * Play only returns offers the user is eligible for, so a free-trial offer present here
     * means this user can still start a trial. Without one we sell the plain base plan.
     */
    fun plans(offers: List<OfferInfo>): List<PlanOption> = offers
        .groupBy { it.basePlanId }
        .mapNotNull { (basePlanId, group) ->
            val chosen = group.maxByOrNull { trialDays(it) } ?: return@mapNotNull null
            val recurring = chosen.phases.lastOrNull { it.priceMicros > 0 } ?: return@mapNotNull null
            val period = when {
                basePlanId == YEARLY || recurring.billingPeriod == "P1Y" || recurring.billingPeriod == "P12M" -> PlanPeriod.YEARLY
                else -> PlanPeriod.MONTHLY
            }
            PlanOption(basePlanId, period, recurring.formattedPrice, recurring.priceMicros, recurring.currencyCode, trialDays(chosen), chosen.offerToken)
        }
        .sortedByDescending { it.period == PlanPeriod.YEARLY }

    fun trialDays(offer: OfferInfo): Int = offer.phases
        .filter { it.priceMicros == 0L }
        .sumOf { isoDays(it.billingPeriod) * maxOf(1, it.cycles) }

    /** ISO-8601 period ("P7D", "P1W", "P1M", "P1Y") in days; months count as 30. */
    fun isoDays(period: String): Int {
        val m = Regex("""P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)W)?(?:(\d+)D)?""").matchEntire(period) ?: return 0
        val (y, mo, w, d) = m.destructured
        return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
    }

    /** "Save 33%": yearly against twelve months, only when both are in the same currency. */
    fun yearlySavingsPercent(monthly: PlanOption?, yearly: PlanOption?): Int? {
        if (monthly == null || yearly == null || monthly.currencyCode != yearly.currencyCode) return null
        val twelve = monthly.priceMicros * 12.0
        if (twelve <= 0) return null
        val pct = ((1 - yearly.priceMicros / twelve) * 100).roundToInt()
        return pct.takeIf { it >= 5 }
    }
}

/**
 * Checks Google's signature on a purchase with the app's Play licence key, so a faked
 * purchase cannot unlock Pro. With no key configured every purchase Play reports is trusted.
 */
object PurchaseVerifier {
    fun isValid(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (base64PublicKey.isBlank()) return true
        if (signedData.isEmpty() || signature.isEmpty()) return false
        return try {
            val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey)))
            Signature.getInstance("SHA1withRSA").run {
                initVerify(key)
                update(signedData.toByteArray())
                verify(Base64.getDecoder().decode(signature))
            }
        } catch (_: Exception) {
            false
        }
    }
}
