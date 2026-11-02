package com.quickfind.core.sources.apps

import android.content.pm.ApplicationInfo

/**
 * What popular apps are *for*, so "payment" finds PhonePe and "taxi" finds Uber (spec 10.3's
 * curated map, India first). Package names that are not installed simply never match.
 */
object AppKeywords {
    private val PAYMENTS = listOf("payment", "pay", "upi", "send money", "wallet", "recharge", "bill")
    private val BANK = listOf("bank", "banking", "account", "balance", "netbanking")
    private val CAB = listOf("taxi", "cab", "ride", "auto", "bike taxi")
    private val FOOD = listOf("food", "order food", "restaurant", "delivery")
    private val GROCERY = listOf("grocery", "groceries", "vegetables", "instant delivery")
    private val SHOPPING = listOf("shopping", "shop", "buy", "order", "clothes")
    private val TRAVEL = listOf("travel", "flight", "hotel", "bus", "train", "ticket", "booking")
    private val OTT = listOf("movies", "tv", "shows", "series", "streaming", "cricket")
    private val MUSIC = listOf("music", "songs", "podcast")
    private val INVEST = listOf("stocks", "shares", "invest", "mutual fund", "trading", "sip")
    private val HEALTH = listOf("medicine", "pharmacy", "doctor", "health", "lab test")
    private val GOV = listOf("government", "documents", "aadhaar", "pan", "certificate")
    private val CHAT = listOf("chat", "message", "messenger")

    private val BY_PACKAGE: Map<String, List<String>> = mapOf(
        "com.phonepe.app" to PAYMENTS,
        "com.google.android.apps.nbu.paisa.user" to PAYMENTS + listOf("gpay", "google pay"),
        "net.one97.paytm" to PAYMENTS + listOf("fastag"),
        "in.org.npci.upiapp" to PAYMENTS + listOf("bhim"),
        "in.amazon.mShop.android.shopping" to SHOPPING + listOf("amazon pay"),
        "com.dreamplug.androidapp" to listOf("credit card", "card bill", "payment", "rent"),
        "com.sbi.lotusintouch" to BANK + listOf("sbi", "yono"),
        "com.snapwork.hdfc" to BANK + listOf("hdfc"),
        "com.csam.icici.bank.imobile" to BANK + listOf("icici"),
        "com.axis.mobile" to BANK + listOf("axis"),
        "com.msf.kbank.mobile" to BANK + listOf("kotak"),
        "com.ubercab" to CAB,
        "com.olacabs.customer" to CAB,
        "com.rapido.passenger" to CAB,
        "in.juspay.nammayatri" to CAB,
        "in.swiggy.android" to FOOD + GROCERY + listOf("instamart"),
        "com.application.zomato" to FOOD,
        "com.grofers.customerapp" to GROCERY + listOf("blinkit"),
        "com.zeptoconsumerapp" to GROCERY,
        "com.bigbasket.mobileapp" to GROCERY,
        "com.flipkart.android" to SHOPPING,
        "com.myntra.android" to SHOPPING + listOf("fashion"),
        "com.meesho.supply" to SHOPPING,
        "com.ril.ajio" to SHOPPING + listOf("fashion"),
        "cris.org.in.prs.ima" to TRAVEL + listOf("irctc", "railway", "pnr"),
        "com.makemytrip" to TRAVEL,
        "com.ixigo.train.ixitrain" to TRAVEL + listOf("pnr", "train status"),
        "in.redbus.android" to TRAVEL,
        "com.bt.bms" to listOf("movies", "tickets", "events", "cinema"),
        "in.startv.hotstar" to OTT + listOf("hotstar"),
        "com.netflix.mediaclient" to OTT,
        "com.amazon.avod.thirdpartyclient" to OTT + listOf("prime video"),
        "com.jio.media.ondemand" to OTT,
        "com.spotify.music" to MUSIC,
        "com.jio.media.jiobeats" to MUSIC + listOf("saavn"),
        "com.gaana" to MUSIC,
        "com.google.android.apps.youtube.music" to MUSIC,
        "com.zerodha.kite3" to INVEST + listOf("zerodha"),
        "com.nextbillion.groww" to INVEST,
        "in.upstox.pro" to INVEST,
        "com.phonegap.rxpal" to HEALTH,
        "com.aranoah.healthkart.plus" to HEALTH + listOf("1mg"),
        "com.practo.fabric" to HEALTH + listOf("appointment"),
        "com.digilocker.android" to GOV,
        "in.gov.umang.negd.g2c" to GOV,
        "in.gov.uidai.mAadhaarPlus" to GOV,
        "com.whatsapp" to CHAT,
        "org.telegram.messenger" to CHAT,
        "com.google.android.gm" to listOf("mail", "email", "inbox"),
        "com.google.android.apps.maps" to listOf("map", "navigation", "directions", "location"),
        "com.google.android.apps.photos" to listOf("photos", "gallery", "pictures"),
    )

    /** Android's own app categories (API 26+). */
    private fun forCategory(category: Int): List<String> = when (category) {
        ApplicationInfo.CATEGORY_GAME -> listOf("game", "games")
        ApplicationInfo.CATEGORY_AUDIO -> MUSIC
        ApplicationInfo.CATEGORY_VIDEO -> listOf("video", "videos")
        ApplicationInfo.CATEGORY_IMAGE -> listOf("photo", "photos", "camera", "editor")
        ApplicationInfo.CATEGORY_SOCIAL -> listOf("social")
        ApplicationInfo.CATEGORY_NEWS -> listOf("news")
        ApplicationInfo.CATEGORY_MAPS -> listOf("map", "navigation")
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> listOf("productivity", "work", "office")
        else -> emptyList()
    }

    fun forApp(packageName: String, category: Int): List<String> =
        (BY_PACKAGE[packageName].orEmpty() + forCategory(category)).distinct()
}
