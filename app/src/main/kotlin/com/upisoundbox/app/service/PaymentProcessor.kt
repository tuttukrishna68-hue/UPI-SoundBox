package com.upisoundbox.app.service

import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.parser.PaymentDeduplicator
import com.upisoundbox.core.parser.PaymentEvent
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val announcementQueue: AnnouncementQueue
) {
    private val deduplicator = PaymentDeduplicator()
    private val processMutex = Mutex()

    suspend fun process(event: PaymentEvent): Boolean = processMutex.withLock {
        // The in-memory window covers a running process; the database check
        // also prevents a replay after the listener/service is recreated.
        val upiRef = event.upiRef?.trim().orEmpty()
        val duplicate = repository.findDuplicate(event.id, upiRef.takeIf { it.isNotEmpty() })
        if (duplicate != null) {
            // When SMS detection wins the race, the saved package can be a
            // bank sender ID and the UI used to show the BHIM fallback icon.
            // Prefer a later confirmed UPI-app notification for the same
            // payment without recording or announcing it a second time.
            if (isKnownUpiApp(event.appPackage) && !isKnownUpiApp(duplicate.appPackage)) {
                try {
                    repository.updateAppPackage(duplicate.id, event.appPackage)
                } catch (_: Exception) {}
            }
            return false
        }
        if (deduplicator.isDuplicate(event)) return false

        try {
            repository.recordPayment(event, announced = true)
        } catch (_: Exception) {
            // Do not announce a payment that was not persisted. The next source
            // callback can retry it instead of creating a voice-only payment.
            return false
        }
        SoundboxEventBus.emitPayment(event)

        // Queue every persisted event immediately. AnnouncementQueue performs
        // the active/quiet-hours/minimum checks in its worker, so DataStore I/O
        // never delays the ledger update or blocks the next notification.
        announcementQueue.enqueue(AnnouncementRequest.Payment(event))
        return true
    }

    private fun isKnownUpiApp(packageName: String): Boolean {
        return when (AnnouncementEngine.mapPackageToDisplayName(packageName)) {
            "UPI", "Bank SMS" -> false
            else -> true
        }
    }
}
