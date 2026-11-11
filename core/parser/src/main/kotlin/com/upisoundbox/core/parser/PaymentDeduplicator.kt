package com.upisoundbox.core.parser

import java.util.concurrent.ConcurrentHashMap

class PaymentDeduplicator(
    var dedupeWindowMs: Long = DEFAULT_WINDOW_MS
) {
    private val seenUpiRefs = ConcurrentHashMap<String, Long>()
    private val recentPayments = ConcurrentHashMap<String, Long>()

    @Synchronized
    fun isDuplicate(event: PaymentEvent, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        prune(currentTimeMs)

        // 1. Check exact UPI Reference if available
        val ref = event.upiRef?.trim()
        if (!ref.isNullOrEmpty()) {
            if (seenUpiRefs.containsKey(ref)) {
                return true
            }
            seenUpiRefs[ref] = currentTimeMs
        }

        // 2. Check (amount + payer) inside dedupe window
        val payerKey = event.payer?.trim()?.lowercase() ?: "unknown"
        val signature = "${event.amountPaise}_$payerKey"

        val lastSeen = recentPayments[signature]
        if (lastSeen != null && (currentTimeMs - lastSeen) <= dedupeWindowMs) {
            return true
        }

        recentPayments[signature] = currentTimeMs
        return false
    }

    @Synchronized
    fun clear() {
        seenUpiRefs.clear()
        recentPayments.clear()
    }

    private fun prune(currentTimeMs: Long) {
        val expiryTime = currentTimeMs - (dedupeWindowMs * 2)
        recentPayments.entries.removeIf { it.value < expiryTime }
        // Keep UPI refs a bit longer (e.g. 10 minutes)
        val refExpiry = currentTimeMs - (10 * 60 * 1000L)
        seenUpiRefs.entries.removeIf { it.value < refExpiry }
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 90_000L // 90 seconds per spec
    }
}

