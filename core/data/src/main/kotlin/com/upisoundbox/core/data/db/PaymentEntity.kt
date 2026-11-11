package com.upisoundbox.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.upisoundbox.core.parser.PaymentEvent
import com.upisoundbox.core.parser.PaymentSource

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val source: String, // NOTIFICATION, SMS, WEBHOOK
    val appPackage: String,
    val amountPaise: Long,
    val payer: String? = null,
    val upiRef: String? = null,
    val receivedAt: Long,
    val announced: Boolean = false,
    val rawSnippet: String? = null
) {
    fun toEvent(): PaymentEvent {
        return PaymentEvent(
            id = id,
            source = try { PaymentSource.valueOf(source) } catch (_: Exception) { PaymentSource.NOTIFICATION },
            appPackage = appPackage,
            amountPaise = amountPaise,
            payer = payer,
            upiRef = upiRef,
            receivedAt = receivedAt
        )
    }

    companion object {
        fun fromEvent(event: PaymentEvent, announced: Boolean = false, rawSnippet: String? = null): PaymentEntity {
            return PaymentEntity(
                id = event.id,
                source = event.source.name,
                appPackage = event.appPackage,
                amountPaise = event.amountPaise,
                payer = event.payer,
                upiRef = event.upiRef,
                receivedAt = event.receivedAt,
                announced = announced,
                rawSnippet = rawSnippet
            )
        }
    }
}

