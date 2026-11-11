package com.upisoundbox.core.parser

data class ExtractedAmount(
    val amountPaise: Long,
    val startIndex: Int,
    val endIndex: Int,
    val rawText: String
)

class AmountParser {
    // Regex matching Indian rupee amounts:
    // Prefixes: ₹, Rs., Rs, INR, INR., with optional spaces
    // Number format: 1,23,456.78 or 123456.78 or 500 or 10.50
    // Suffixes: /-, /-INR, etc.
    private val amountRegex = Regex(
        "(?:(?:₹|Rs\\.?|INR)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)|([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:₹|Rs\\.?|INR))",
        RegexOption.IGNORE_CASE
    )

    fun findAllAmounts(text: String): List<ExtractedAmount> {
        val matches = amountRegex.findAll(text)
        val results = mutableListOf<ExtractedAmount>()

        for (match in matches) {
            val numStr = match.groups[1]?.value ?: match.groups[2]?.value ?: continue
            val paise = parseToPaise(numStr) ?: continue
            if (paise > 0) {
                results.add(
                    ExtractedAmount(
                        amountPaise = paise,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1,
                        rawText = match.value
                    )
                )
            }
        }
        return results
    }

    fun findBestAmount(text: String, creditKeywordPositions: List<Int>): ExtractedAmount? {
        val amounts = findAllAmounts(text)
        if (amounts.isEmpty()) return null
        if (amounts.size == 1) return amounts.first()

        // Multiple amounts: pick the one closest to a credit keyword
        if (creditKeywordPositions.isEmpty()) {
            return amounts.first()
        }

        return amounts.minByOrNull { amt ->
            val amtCenter = (amt.startIndex + amt.endIndex) / 2
            creditKeywordPositions.minOf { kwPos -> kotlin.math.abs(amtCenter - kwPos) }
        }
    }

    companion object {
        fun parseToPaise(cleanNumberStr: String): Long? {
            val sanitized = cleanNumberStr.replace(",", "").trim()
            if (sanitized.isEmpty()) return null

            val parts = sanitized.split(".")
            val rupees = parts[0].toLongOrNull() ?: return null

            val paise = if (parts.size > 1) {
                val paiseStr = parts[1].padEnd(2, '0').take(2)
                paiseStr.toLongOrNull() ?: 0L
            } else {
                0L
            }

            return rupees * 100L + paise
        }

        fun formatPaise(paise: Long): String {
            val rupees = paise / 100
            val remainder = paise % 100
            val formattedRupees = formatIndianGrouping(rupees)
            return if (remainder > 0) {
                "₹$formattedRupees.%02d".format(remainder)
            } else {
                "₹$formattedRupees"
            }
        }

        private fun formatIndianGrouping(value: Long): String {
            if (value < 1000) return value.toString()
            val str = value.toString()
            val lastThree = str.takeLast(3)
            val rest = str.dropLast(3)
            val chunks = mutableListOf<String>()
            var i = rest.length
            while (i > 0) {
                val start = (i - 2).coerceAtLeast(0)
                chunks.add(0, rest.substring(start, i))
                i -= 2
            }
            return chunks.joinToString(",") + "," + lastThree
        }
    }
}

