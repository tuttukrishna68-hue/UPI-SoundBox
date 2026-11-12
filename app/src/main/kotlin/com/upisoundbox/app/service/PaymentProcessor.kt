package com.upisoundbox.app.service

import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.parser.PaymentDeduplicator
import com.upisoundbox.core.parser.PaymentEvent
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for payment events from notifications and SMS.
 *
 * Keeping deduplication, persistence and announcement dispatch in one
 * application-scoped processor prevents the same payment from being handled
 * twice when both sources report it.
 */
@Singleton
class PaymentProcessor @Inject constructor(
    private val repository: PaymentRepository,
    private val preferences: SoundboxPreferences,
    private val announcementQueue: AnnouncementQueue
) {
    private val deduplicator = PaymentDeduplicator()

    suspend fun process(event: PaymentEvent): Boolean {
        // The in-memory window covers a running process; the database check
        // also prevents a replay after the listener/service is recreated.
        if (repository.getPaymentById(event.id) != null) return false
        val upiRef = event.upiRef?.trim().orEmpty()
        if (upiRef.isNotEmpty() && repository.getPaymentByUpiRef(upiRef) != null) return false
        if (deduplicator.isDuplicate(event)) return false

        repository.recordPayment(event, announced = true)
        SoundboxEventBus.emitPayment(event)

        // Pausing the speaker stops audio while the ledger continues to retain
        // valid incoming payments for an accurate collection total.
        if (preferences.isSoundboxActive.first()) {
            announcementQueue.enqueue(AnnouncementRequest.Payment(event))
        }
        return true
    }
}
