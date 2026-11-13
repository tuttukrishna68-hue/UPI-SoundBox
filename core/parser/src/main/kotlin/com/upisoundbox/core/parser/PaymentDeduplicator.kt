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

        // 2. Check a conservative fallback signature inside the dedupe window.
        // A missing payer is common in UPI notifications. Treating every such
        // payment as "unknown" made two real payments of the same amount look
        // like one transaction. Without a payer or UPI reference there is no
        // reliable business identity to dedupe, so let the event through.
        val payerKey = event.payer?.trim()?.lowercase() ?: return false
        if (payerKey.isBlank() || payerKey in UNIDENTIFIED_PAYERS) {
            return false
        }
        val signature = "${event.source.name}:${event.appPackage.lowercase()}:${event.amountPaise}_$payerKey"

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
        private val UNIDENTIFIED_PAYERS = setOf("unknown", "customer", "payer")
    }
}


