package com.quickfind.core.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PlanCatalogTest {
    private fun phase(micros: Long, period: String, cycles: Int = 0) =
        PhaseInfo(micros, if (micros == 0L) "Free" else "$" + "%.2f".format(micros / 1e6), "USD", period, cycles)

    private val monthlyBase = OfferInfo("monthly", null, "m-base", listOf(phase(4_990_000, "P1M")))
    private val monthlyTrial = OfferInfo("monthly", "free-trial", "m-trial", listOf(phase(0, "P1W", 1), phase(4_990_000, "P1M")))
    private val yearlyBase = OfferInfo("yearly", null, "y-base", listOf(phase(39_990_000, "P1Y")))
    private val yearlyTrial = OfferInfo("yearly", "free-trial", "y-trial", listOf(phase(0, "P7D", 1), phase(39_990_000, "P1Y")))

    @Test
    fun picksTheTrialOfferWhenEligibleAndListsYearlyFirst() {
        val plans = PlanCatalog.plans(listOf(monthlyBase, monthlyTrial, yearlyBase, yearlyTrial))
        assertEquals(listOf(PlanPeriod.YEARLY, PlanPeriod.MONTHLY), plans.map { it.period })
        assertEquals("y-trial", plans[0].offerToken)
        assertEquals(7, plans[0].freeTrialDays)
        assertEquals("m-trial", plans[1].offerToken)
        assertEquals(4_990_000, plans[1].priceMicros)
        assertEquals("$4.99", plans[1].formattedPrice)
    }

    @Test
    fun withoutATrialOfferItSellsTheBasePlan() {
        val plans = PlanCatalog.plans(listOf(monthlyBase, yearlyBase))
        assertTrue(plans.none { it.hasFreeTrial })
        assertEquals("m-base", plans.first { it.period == PlanPeriod.MONTHLY }.offerToken)
    }

    @Test
    fun isoPeriods() {
        assertEquals(7, PlanCatalog.isoDays("P7D"))
        assertEquals(7, PlanCatalog.isoDays("P1W"))
        assertEquals(30, PlanCatalog.isoDays("P1M"))
        assertEquals(365, PlanCatalog.isoDays("P1Y"))
        assertEquals(0, PlanCatalog.isoDays("bogus"))
    }

    @Test
    fun yearlySavings() {
        val plans = PlanCatalog.plans(listOf(monthlyBase, yearlyBase))
        val m = plans.first { it.period == PlanPeriod.MONTHLY }
        val y = plans.first { it.period == PlanPeriod.YEARLY }
        assertEquals(33, PlanCatalog.yearlySavingsPercent(m, y))
        assertNull(PlanCatalog.yearlySavingsPercent(m, y.copy(currencyCode = "INR")))
        assertNull(PlanCatalog.yearlySavingsPercent(null, y))
    }

    @Test
    fun verifierAcceptsGoogleSignedDataAndRejectsForgeries() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
        val key = Base64.getEncoder().encodeToString(pair.public.encoded)
        val data = """{"productId":"findpal_pro","purchaseState":0}"""
        val sig = Signature.getInstance("SHA1withRSA").run {
            initSign(pair.private)
            update(data.toByteArray())
            Base64.getEncoder().encodeToString(sign())
        }
        assertTrue(PurchaseVerifier.isValid(key, data, sig))
        assertFalse(PurchaseVerifier.isValid(key, data.replace("0", "1"), sig))
        assertFalse(PurchaseVerifier.isValid(key, data, ""))
        assertTrue("no key configured: trust Play", PurchaseVerifier.isValid("", data, ""))
    }
}
