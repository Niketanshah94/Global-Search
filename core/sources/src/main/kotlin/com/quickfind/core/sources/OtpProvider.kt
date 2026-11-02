package com.quickfind.core.sources

/** A source that can offer the most recent one-time code ("Copy OTP" chip). */
interface OtpProvider {
    data class Code(val code: String, val source: String)

    fun latestOtp(): Code?
}
