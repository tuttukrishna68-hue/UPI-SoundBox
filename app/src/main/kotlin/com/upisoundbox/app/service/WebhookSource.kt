package com.upisoundbox.app.service

import com.upisoundbox.core.parser.PaymentEvent
import com.upisoundbox.core.parser.PaymentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * WebhookSource Stub.
 *
 * Designed for future cloud gateway, webhook push, or Firebase Cloud Messaging (FCM)
 * integrations. When enabled in future releases, an external payment aggregator
 * (e.g., merchant payment gateway or local server) can push raw payment events directly
 * to this source.
 *
 * Current implementation is a documented no-op stub that emits an empty stream,
 * preserving zero-internet and privacy-first local architecture for v1.
 */
interface WebhookSource {
    /**
     * Observable stream of incoming webhook payment signals.
     */
    val webhookEvents: Flow<PaymentEvent>

    /**
     * Start listening for gateway webhooks or local TCP sockets.
     */
    fun startListening()

    /**
     * Stop listening.
     */
    fun stopListening()
}

class WebhookSourceStub : WebhookSource {
    override val webhookEvents: Flow<PaymentEvent> = emptyFlow()

    override fun startListening() {
        // TODO: Implement FCM / local socket listener when cloud gateway is supported.
    }

    override fun stopListening() {
        // TODO: Clean up network connections when implemented.
    }
}

