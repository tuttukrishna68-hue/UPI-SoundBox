package com.upisoundbox.core.parser

import java.util.UUID

interface ParserStrategy {
    val parserId: String
    fun parse(notification: RawNotification): ParseResult
}

abstract class BaseNotificationParser(
    override val parserId: String,
    protected val keywordMatcher: KeywordMatcher = KeywordMatcher(),
    protected val amountParser: AmountParser = AmountParser(),
    protected val payerParser: PayerParser = PayerParser()
) : ParserStrategy {

    override fun parse(notification: RawNotification): ParseResult {
        if (notification.isGroupSummary) {
            return ParseResult.Ignored("Group summary notification")
        }
        if (notification.isOngoing) {
            return ParseResult.Ignored("Ongoing notification")
        }

        val fullText = notification.fullText()
        if (fullText.isBlank()) {
            return ParseResult.Ignored("Empty notification text")
        }

        // 1. Check reject keywords first
        val rejectedKeyword = keywordMatcher.hasRejectKeyword(fullText)
        if (rejectedKeyword != null) {
            return ParseResult.Rejected(
                reason = "Matched debit/noise keyword: '$rejectedKeyword'",
                rawSnippet = fullText.take(100)
            )
        }

        // 2. Check incoming money / credit keywords
        val creditMatches = keywordMatcher.findCreditKeywords(fullText)
        if (creditMatches.isEmpty() && !hasAppSpecificCreditIntent(notification)) {
            return ParseResult.Rejected(
                reason = "No incoming money intent detected",
                rawSnippet = fullText.take(100)
            )
        }

        // 3. Extract amount
        val creditPositions = creditMatches.map { (it.range.first + it.range.last) / 2 }
        val extractedAmount = amountParser.findBestAmount(fullText, creditPositions)
            ?: return ParseResult.Rejected("Could not extract valid amount", fullText.take(100))

        // 4. Extract payer and UPI Ref
        val payerInfo = extractPayerInfo(notification, fullText)

        val event = PaymentEvent(
            id = generateEventId(notification),
            source = PaymentSource.NOTIFICATION,
            appPackage = notification.packageName,
            amountPaise = extractedAmount.amountPaise,
            payer = payerInfo.payer,
            upiRef = payerInfo.upiRef,
            receivedAt = if (notification.postTime > 0) notification.postTime else System.currentTimeMillis()
        )

        return ParseResult.Success(event)
    }

    protected open fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean = false

    protected open fun extractPayerInfo(notification: RawNotification, fullText: String): ExtractedPayerInfo {
        return payerParser.extract(fullText)
    }

    protected open fun generateEventId(notification: RawNotification): String {
        return "${notification.packageName}_${notification.key}_${notification.postTime}"
    }
}

class GooglePayParser : BaseNotificationParser("google_pay") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val title = notification.title?.lowercase() ?: ""
        val text = notification.text?.lowercase() ?: ""
        return title.contains("paid you") || text.contains("paid you") ||
                title.contains("received") || text.contains("received")
    }

    override fun extractPayerInfo(notification: RawNotification, fullText: String): ExtractedPayerInfo {
        // In GPay, title often has the payer: "Rahul Sharma paid you ₹500"
        val title = notification.title ?: ""
        val titleMatch = Regex("^([A-Za-z\\s]+)\\s+paid you", RegexOption.IGNORE_CASE).find(title)
        if (titleMatch != null) {
            val payerName = PayerParser.cleanPayerName(titleMatch.groupValues[1])
            val baseInfo = payerParser.extract(fullText)
            return baseInfo.copy(payer = payerName ?: baseInfo.payer)
        }
        return super.extractPayerInfo(notification, fullText)
    }
}

class GooglePayBusinessParser : BaseNotificationParser("google_pay_business") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("payment received") || full.contains("received") || full.contains("credited")
    }
}

class PhonePeParser : BaseNotificationParser("phonepe") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("payment received") || full.contains("received") || full.contains("transferred")
    }
}

class PhonePeBusinessParser : BaseNotificationParser("phonepe_business") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("payment received") || full.contains("received") || full.contains("credited")
    }
}

class PaytmParser : BaseNotificationParser("paytm") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("received ₹") || full.contains("received rs") || full.contains("money received")
    }
}

class PaytmBusinessParser : BaseNotificationParser("paytm_business") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("received ₹") || full.contains("payment received") || full.contains("credited")
    }
}

class BhimParser : BaseNotificationParser("bhim") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("credited") || full.contains("received")
    }
}

class AmazonPayParser : BaseNotificationParser("amazon_pay") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("received") || full.contains("credited") || full.contains("added")
    }
}

class CredParser : BaseNotificationParser("cred") {
    override fun hasAppSpecificCreditIntent(notification: RawNotification): Boolean {
        val full = notification.fullText().lowercase()
        return full.contains("received") || full.contains("credited") || full.contains("cashback received")
    }
}

class GenericFallbackParser : BaseNotificationParser("generic")

class SmsParser(
    private val keywordMatcher: KeywordMatcher = KeywordMatcher(),
    private val amountParser: AmountParser = AmountParser(),
    private val payerParser: PayerParser = PayerParser()
) {
    fun parse(sms: RawSms): ParseResult {
        val body = sms.body
        if (body.isBlank()) {
            return ParseResult.Ignored("Empty SMS body")
        }

        // Check reject keywords
        val rejected = keywordMatcher.hasRejectKeyword(body)
        if (rejected != null) {
            return ParseResult.Rejected("SMS matched reject keyword: $rejected", body.take(80))
        }

        // Check credit intent
        val creditMatches = keywordMatcher.findCreditKeywords(body)
        if (creditMatches.isEmpty()) {
            return ParseResult.Rejected("No credit keyword in SMS", body.take(80))
        }

        // Extract amount
        val creditPositions = creditMatches.map { (it.range.first + it.range.last) / 2 }
        val extractedAmount = amountParser.findBestAmount(body, creditPositions)
            ?: return ParseResult.Rejected("No valid amount found in SMS", body.take(80))

        val payerInfo = payerParser.extract(body)

        val event = PaymentEvent(
            id = "sms_${sms.sender}_${sms.receivedAt}_${UUID.randomUUID().toString().take(8)}",
            source = PaymentSource.SMS,
            appPackage = sms.sender,
            amountPaise = extractedAmount.amountPaise,
            payer = payerInfo.payer,
            upiRef = payerInfo.upiRef,
            receivedAt = sms.receivedAt
        )

        return ParseResult.Success(event)
    }
}

