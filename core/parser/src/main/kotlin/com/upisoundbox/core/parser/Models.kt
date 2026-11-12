package com.upisoundbox.core.parser

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentSource {
    NOTIFICATION,
    SMS,
    WEBHOOK
}

@Serializable
data class PaymentEvent(
    val id: String,
    val source: PaymentSource,
    val appPackage: String,
    val amountPaise: Long,
    val payer: String? = null,
    val upiRef: String? = null,
    val receivedAt: Long = System.currentTimeMillis()
)

data class RawNotification(
    val key: String,
    val packageName: String,
    val postTime: Long,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val textLines: List<String> = emptyList(),
    val isGroupSummary: Boolean = false,
    val isOngoing: Boolean = false
) {
    fun fullText(): String {
        return buildList {
            title?.let { add(it) }
            text?.let { add(it) }
            bigText?.let { add(it) }
            subText?.let { add(it) }
            addAll(textLines)
        }.joinToString(separator = " \n ")
    }
}

data class RawSms(
    val sender: String,
    val body: String,
    val receivedAt: Long = System.currentTimeMillis()
)

sealed interface ParseResult {
    data class Success(val event: PaymentEvent) : ParseResult
    data class Rejected(val reason: String, val rawSnippet: String = "") : ParseResult
    data class Ignored(val reason: String) : ParseResult
}

@Serializable
data class UpiAppConfig(
    val packageId: String,
    val displayName: String,
    val enabled: Boolean,
    val parserId: String,
    val verified: Boolean = false,
    val comment: String? = null
)

