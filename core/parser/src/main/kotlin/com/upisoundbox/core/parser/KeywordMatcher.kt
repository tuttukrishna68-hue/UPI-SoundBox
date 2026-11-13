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

    private val creditKeywordRegexes: List<Regex> = creditKeywords.map(::keywordRegex)
    private val rejectKeywordRegexes: List<Regex> = rejectKeywords.map(::keywordRegex)

    fun hasRejectKeyword(text: String): String? {
        val lower = text.lowercase()
        return rejectKeywords.zip(rejectKeywordRegexes).firstOrNull { (_, regex) ->
            regex.containsMatchIn(lower)
        }?.first
    }

    fun findCreditKeywords(text: String): List<MatchResult> {
        val lower = text.lowercase()
        val matches = mutableListOf<MatchResult>()
        for (regex in creditKeywordRegexes) {
            var match = regex.find(lower)
            while (match != null) {
                matches.add(match)
                match = match.next()
            }
        }
        return matches
    }

    private fun keywordRegex(keyword: String): Regex {
        return Regex("(?:^|[^a-z0-9])${Regex.escape(keyword)}(?:$|[^a-z0-9])")
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
            "transferred to", "deposit of", "has sent", "has transferred",
            "transferred ₹", "transferred rs", "sent ₹", "sent rs",
            "received a payment", "payment of"
        )

        private val defaultRejectKeywords = listOf(
            "you paid", "paid to", "sent to", "debited", "debited from",
            "payment failed", "transaction failed", "payment declined", "transaction declined",
            "transaction pending", "payment pending", "payment cancelled", "transaction cancelled",
            "refund issued", "refund completed", "bill due", "due date", "overdue",
            "otp", "verification code", "security code",
            "recharge successful", "autopay", "auto-debit", "mandate",
            "split bill", "statement", "summary of the day"
        )
    }
}

