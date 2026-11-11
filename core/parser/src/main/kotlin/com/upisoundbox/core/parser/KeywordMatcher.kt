package com.upisoundbox.core.parser

class KeywordMatcher(
    creditKeywordsStream: List<String>? = null,
    rejectKeywordsStream: List<String>? = null
) {
    val creditKeywords: List<String> = creditKeywordsStream
        ?: loadLinesFromResource("credit_keywords.txt")
        ?: defaultCreditKeywords

    val rejectKeywords: List<String> = rejectKeywordsStream
        ?: loadLinesFromResource("reject_keywords.txt")
        ?: defaultRejectKeywords

    fun hasRejectKeyword(text: String): String? {
        val lower = text.lowercase()
        return rejectKeywords.firstOrNull { keyword ->
            val regex = Regex("(?:^|[^a-z0-9])${Regex.escape(keyword)}(?:$|[^a-z0-9])")
            regex.containsMatchIn(lower)
        }
    }

    fun findCreditKeywords(text: String): List<MatchResult> {
        val lower = text.lowercase()
        val matches = mutableListOf<MatchResult>()
        for (kw in creditKeywords) {
            val regex = Regex("(?:^|[^a-z0-9])${Regex.escape(kw)}(?:$|[^a-z0-9])")
            var match = regex.find(lower)
            while (match != null) {
                matches.add(match)
                match = match.next()
            }
        }
        return matches
    }

    companion object {
        private fun loadLinesFromResource(filename: String): List<String>? {
            return try {
                val stream = KeywordMatcher::class.java.classLoader?.getResourceAsStream(filename)
                    ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(filename)
                stream?.bufferedReader()?.useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .toList()
                }
            } catch (_: Exception) {
                null
            }
        }

        private val defaultCreditKeywords = listOf(
            "received", "received from", "paid you", "credited", "credited to",
            "credited with", "sent you", "received in", "added to", "payment received",
            "transferred to", "deposit of", "has sent"
        )

        private val defaultRejectKeywords = listOf(
            "you paid", "paid to", "sent to", "debited", "debited from",
            "request", "requested", "reminder", "reminding", "failed",
            "declined", "pending", "cancelled", "refund", "cashback",
            "reward", "offer", "scratch card", "bill due", "due date",
            "overdue", "otp", "verification code", "security code",
            "recharge successful", "autopay", "auto-debit", "mandate",
            "split bill", "statement", "summary of the day"
        )
    }
}

