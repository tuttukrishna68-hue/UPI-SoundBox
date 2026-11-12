package com.upisoundbox.app.service

import com.upisoundbox.core.parser.PaymentEvent
import com.upisoundbox.core.parser.RawNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SoundboxEventBus {
    private val _paymentEvents = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 64)
    val paymentEvents = _paymentEvents.asSharedFlow()

    private val _unknownAppNotifications = MutableSharedFlow<RawNotification>(extraBufferCapacity = 32)
    val unknownAppNotifications = _unknownAppNotifications.asSharedFlow()

    fun emitPayment(event: PaymentEvent) {
        _paymentEvents.tryEmit(event)
    }

    fun emitUnknownNotification(notification: RawNotification) {
        _unknownAppNotifications.tryEmit(notification)
    }
}

