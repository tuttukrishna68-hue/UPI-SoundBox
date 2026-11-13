package com.upisoundbox.core.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeduplicatorTest {
    private val deduplicator = PaymentDeduplicator(dedupeWindowMs = 90_000L)

    @Test
    fun dedupesExactSameUpiRefAcrossSources() {
        val event1 = PaymentEvent(
            id = "event_1",
            source = PaymentSource.NOTIFICATION,
            appPackage = "com.google.android.apps.nbu.paisa.user",
            amountPaise = 50000L,
            payer = "Rahul",
            upiRef = "412345678901"
        )
        val event2 = PaymentEvent(
            id = "event_2",
            source = PaymentSource.SMS,
            appPackage = "VK-HDFCBK",
            amountPaise = 50000L,
            payer = "Rahul",
            upiRef = "412345678901"
        )

        assertThat(deduplicator.isDuplicate(event1, 1000L)).isFalse()
        assertThat(deduplicator.isDuplicate(event2, 2000L)).isTrue()
    }

    @Test
    fun dedupesCrossSourceSameAmountAndPayerInsideWindow() {
        val event1 = PaymentEvent(
            id = "event_1",
            source = PaymentSource.SMS,
            appPackage = "VK-HDFCBK",
            amountPaise = 10000L,
            payer = "Suresh"
        )
        val event2 = PaymentEvent(
            id = "event_2",
            source = PaymentSource.NOTIFICATION,
            appPackage = "com.phonepe.app",
            amountPaise = 10000L,
            payer = "Suresh"
        )

        assertThat(deduplicator.isDuplicate(event1, 1000L)).isFalse()
        // 5 seconds later -> duplicate
        assertThat(deduplicator.isDuplicate(event2, 6000L)).isTrue()
        // 100 seconds later -> outside 90s window -> not a duplicate
        assertThat(deduplicator.isDuplicate(event2, 98000L)).isFalse()
    }

    @Test
    fun doesNotDedupeDifferentAmounts() {
        val event1 = PaymentEvent(
            id = "event_1",
            source = PaymentSource.NOTIFICATION,
            appPackage = "net.one97.paytm",
            amountPaise = 5000L,
            payer = "Pooja"
        )
        val event2 = PaymentEvent(
            id = "event_2",
            source = PaymentSource.NOTIFICATION,
            appPackage = "net.one97.paytm",
            amountPaise = 10000L,
            payer = "Pooja"
        )

        assertThat(deduplicator.isDuplicate(event1, 1000L)).isFalse()
        assertThat(deduplicator.isDuplicate(event2, 2000L)).isFalse()
    }

    @Test
    fun allowsSeparatePaymentsWithSameAmountAndPayerFromSameSource() {
        val event1 = PaymentEvent(
            id = "event_1",
            source = PaymentSource.NOTIFICATION,
            appPackage = "com.google.android.apps.nbu.paisa.user",
            amountPaise = 10000L,
            payer = "Suresh"
        )
        val event2 = event1.copy(id = "event_2")

        assertThat(deduplicator.isDuplicate(event1, 1000L)).isFalse()
        assertThat(deduplicator.isDuplicate(event2, 2000L)).isFalse()
    }

    @Test
    fun allowsSeparatePaymentsWithSameAmountWhenPayerIsMissing() {
        val event1 = PaymentEvent(
            id = "event_1",
            source = PaymentSource.NOTIFICATION,
            appPackage = "com.google.android.apps.nbu.paisa.user",
            amountPaise = 10000L,
            payer = null
        )
        val event2 = event1.copy(id = "event_2")

        assertThat(deduplicator.isDuplicate(event1, 1000L)).isFalse()
        assertThat(deduplicator.isDuplicate(event2, 2000L)).isFalse()
    }
}

