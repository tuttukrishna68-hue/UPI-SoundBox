package com.upisoundbox.core.parser

data class ExtractedPayerInfo(
    val payer: String? = null,
    val upiRef: String? = null
)

class PayerParser {
    // UPI VPA pattern: username@bank
    private val vpaRegex = Regex("([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)")

    // 12-digit UPI reference / RRN pattern
    private val upiRefRegex = Regex("(?:UPI\\s*Ref(?:\\.?|erence)?(?:\\s*No\\.?)?|RRN|Ref(?:\\s*No\\.?)?|Txn\\s*Id|UTR)\\s*[:\\-]?\\s*([0-9]{12}|[A-Za-z0-9]{12})", RegexOption.IGNORE_CASE)

    // Name patterns: "from <Name>", "paid by <Name>", "sent by <Name>"
    private val fromNameRegex = Regex("(?:from|by|received from)\\s+([A-Z][a-zA-Z\\s]{2,30}?)(?:\\s+(?:is|has|was|via|using|on|to|for|with|ref|upi|credited)|[.,\\n]|$)", RegexOption.IGNORE_CASE)

    fun extract(text: String): ExtractedPayerInfo {
        val upiRef = upiRefRegex.find(text)?.groups?.get(1)?.value

        // Check for VPA first
        val vpaMatch = vpaRegex.find(text)?.groups?.get(1)?.value
        if (vpaMatch != null) {
            val maskedVpa = maskVpa(vpaMatch)
            return ExtractedPayerInfo(payer = maskedVpa, upiRef = upiRef)
        }

        // Try extracting name after "from" / "by"
        val nameMatch = fromNameRegex.find(text)?.groups?.get(1)?.value?.trim()
        val cleanName = cleanPayerName(nameMatch)

        return ExtractedPayerInfo(payer = cleanName, upiRef = upiRef)
    }

    companion object {
        fun maskVpa(vpa: String): String {
            val parts = vpa.split("@")
            if (parts.size != 2) return vpa
            val handle = parts[0]
            val bank = parts[1]

            val maskedHandle = when {
                handle.length <= 2 -> "${handle}****"
                handle.length == 3 -> "${handle.take(2)}****"
                else -> "${handle.take(3)}****${handle.takeLast(1)}"
            }
            return "$maskedHandle@$bank"
        }

        fun cleanPayerName(name: String?): String? {
            if (name.isNullOrBlank()) return null
            val trimmed = name.trim().replace(Regex("\\s+"), " ")
            // Exclude noise words
            val lower = trimmed.lowercase()
            if (lower in listOf("you", "your", "account", "bank", "upi", "card", "wallet", "merchant")) {
                return null
            }
            return trimmed
        }
    }
}

