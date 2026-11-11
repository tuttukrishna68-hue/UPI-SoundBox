package com.upisoundbox.core.parser

object NumberToWords {

    fun convertPaise(amountPaise: Long, languageTag: String): String {
        if (amountPaise <= 0) return ""

        val rupees = amountPaise / 100
        val paise = amountPaise % 100

        return when (languageTag.lowercase().replace("_", "-")) {
            "en-in", "en" -> convertEnglish(rupees, paise)
            "hi-in", "hi" -> convertHindi(rupees, paise)
            "ml-in", "ml" -> convertMalayalam(rupees, paise)
            "ta-in", "ta" -> convertTamil(rupees, paise)
            "te-in", "te" -> convertTelugu(rupees, paise)
            "kn-in", "kn" -> convertKannada(rupees, paise)
            else -> convertEnglish(rupees, paise)
        }
    }

    // --- English (India) ---
    private fun convertEnglish(rupees: Long, paise: Long): String {
        val parts = mutableListOf<String>()
        if (rupees > 0) {
            val words = convertIndianNumberEnglish(rupees)
            parts.add("$words ${if (rupees == 1L) "rupee" else "rupees"}")
        }
        if (paise > 0) {
            val paiseWords = convertLessThanHundredEnglish(paise.toInt())
            parts.add("$paiseWords ${if (paise == 1L) "paisa" else "paise"}")
        }
        return parts.joinToString(" and ")
    }

    private fun convertIndianNumberEnglish(n: Long): String {
        if (n == 0L) return "Zero"
        val parts = mutableListOf<String>()

        val crore = n / 10_000_000L
        var remainder = n % 10_000_000L
        if (crore > 0) {
            parts.add("${convertIndianNumberEnglish(crore)} Crore")
        }

        val lakh = remainder / 100_000L
        remainder %= 100_000L
        if (lakh > 0) {
            parts.add("${convertLessThanHundredEnglish(lakh.toInt())} Lakh")
        }

        val thousand = remainder / 1000L
        remainder %= 1000L
        if (thousand > 0) {
            parts.add("${convertLessThanHundredEnglish(thousand.toInt())} Thousand")
        }

        val hundred = remainder / 100L
        remainder %= 100L
        if (hundred > 0) {
            parts.add("${convertLessThanHundredEnglish(hundred.toInt())} Hundred")
        }

        if (remainder > 0) {
            parts.add(convertLessThanHundredEnglish(remainder.toInt()))
        }

        return parts.joinToString(" ")
    }

    private val enUnits = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )
    private val enTens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private fun convertLessThanHundredEnglish(num: Int): String {
        return when {
            num < 20 -> enUnits[num]
            num % 10 == 0 -> enTens[num / 10]
            else -> "${enTens[num / 10]} ${enUnits[num % 10]}"
        }
    }

    // --- Hindi ---
    private fun convertHindi(rupees: Long, paise: Long): String {
        val parts = mutableListOf<String>()
        if (rupees > 0) {
            parts.add("${convertIndianNumberHindi(rupees)} रुपये")
        }
        if (paise > 0) {
            parts.add("${convertHindiBelow100(paise.toInt())} पैसे")
        }
        return parts.joinToString(" ")
    }

    private fun convertIndianNumberHindi(n: Long): String {
        if (n == 0L) return "शून्य"
        val parts = mutableListOf<String>()

        val crore = n / 10_000_000L
        var rem = n % 10_000_000L
        if (crore > 0) parts.add("${convertIndianNumberHindi(crore)} करोड़")

        val lakh = rem / 100_000L
        rem %= 100_000L
        if (lakh > 0) parts.add("${convertHindiBelow100(lakh.toInt())} लाख")

        val thousand = rem / 1000L
        rem %= 1000L
        if (thousand > 0) parts.add("${convertHindiBelow100(thousand.toInt())} हज़ार")

        val hundred = rem / 100L
        rem %= 100L
        if (hundred > 0) parts.add("${convertHindiBelow100(hundred.toInt())} सौ")

        if (rem > 0) parts.add(convertHindiBelow100(rem.toInt()))

        return parts.joinToString(" ")
    }

    private val hiNumbers = arrayOf(
        "", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
        "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
        "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाईस", "उनतीस", "तीस",
        "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस", "चालीस",
        "इकतालीस", "बयालीस", "तैंतालीस", "चवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास", "पचास",
        "इक्यावन", "बावन", "तिरेपन", "चौवन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ", "साठ",
        "इकसठ", "बासठ", "तिरसठ", "चौंसठ", "पैंसठ", "छियासठ", "सरसठ", "अड़सठ", "उनहत्तर", "सत्तर",
        "इकहत्तर", "बहत्तर", "तिहत्तर", "चौहत्तर", "पचहत्तर", "छिहत्तर", "सतहत्तर", "अठहत्तर", "उन्नासी", "अस्सी",
        "इक्यासी", "बयासी", "तिरासी", "चौरासी", "पचासी", "छियासी", "सत्तासी", "अट्ठासी", "नवासी", "नब्बे",
        "इक्यानवे", "बानवे", "तिरानवे", "चौरानवे", "पंचानवे", "छियानवे", "सत्तानवे", "अट्ठानवे", "निन्यानवे"
    )

    private fun convertHindiBelow100(num: Int): String {
        return if (num in 1..99) hiNumbers[num] else ""
    }

    // --- Malayalam ---
    private fun convertMalayalam(rupees: Long, paise: Long): String {
        val parts = mutableListOf<String>()
        if (rupees > 0) parts.add("${convertIndianNumberMalayalam(rupees)} രൂപ")
        if (paise > 0) parts.add("${convertMalayalamBelow100(paise.toInt())} പൈസ")
        return parts.joinToString(" ")
    }

    private fun convertIndianNumberMalayalam(n: Long): String {
        if (n == 0L) return "പൂജ്യം"
        val parts = mutableListOf<String>()

        val crore = n / 10_000_000L
        var rem = n % 10_000_000L
        if (crore > 0) {
            val prefix = if (crore == 1L) "ഒരു" else convertIndianNumberMalayalam(crore)
            parts.add("$prefix കോടി")
        }

        val lakh = rem / 100_000L
        rem %= 100_000L
        if (lakh > 0) {
            val prefix = if (lakh == 1L) "ഒരു" else convertMalayalamBelow100(lakh.toInt())
            parts.add("$prefix ലക്ഷം")
        }

        val thousand = rem / 1000L
        rem %= 1000L
        if (thousand > 0) {
            val prefix = if (thousand == 1L) "ഒരു" else convertMalayalamBelow100(thousand.toInt())
            parts.add("$prefix ആയിരം")
        }

        val hundred = rem / 100L
        rem %= 100L
        if (hundred > 0) {
            val prefix = if (hundred == 1L) "ഒരു" else convertMalayalamBelow100(hundred.toInt())
            parts.add("$prefix നൂറ്")
        }

        if (rem > 0) parts.add(convertMalayalamBelow100(rem.toInt()))

        return parts.joinToString(" ")
    }

    private val mlUnits = arrayOf("", "ഒന്ന്", "രണ്ട്", "മൂന്ന്", "നാല്", "അഞ്ച്", "ആറ്", "ഏഴ്", "എട്ട്", "ഒൻപത്")
    private val mlTeens = arrayOf("പത്ത്", "പതിനൊന്ന്", "പന്ത്രണ്ട്", "പതിമൂന്ന്", "പതിനാല്", "പതിനഞ്ച്", "പതിനാറ്", "പതിനേഴ്", "പതിനെട്ട്", "പത്തൊൻപത്")
    private val mlTens = arrayOf("", "പത്ത്", "ഇരുപത്", "മുപ്പത്", "നാൽപ്പത്", "അമ്പത്", "അറുപത്", "എഴുപത്", "എൺപത്", "തൊണ്ണൂറ്")

    private fun convertMalayalamBelow100(n: Int): String {
        return when {
            n == 0 -> ""
            n < 10 -> mlUnits[n]
            n < 20 -> mlTeens[n - 10]
            n % 10 == 0 -> mlTens[n / 10]
            else -> "${mlTens[n / 10]} ${mlUnits[n % 10]}"
        }
    }

    // --- Tamil ---
    private fun convertTamil(rupees: Long, paise: Long): String {
        val parts = mutableListOf<String>()
        if (rupees > 0) parts.add("${convertIndianNumberTamil(rupees)} ரூபாய்")
        if (paise > 0) parts.add("${convertTamilBelow100(paise.toInt())} பைசா")
        return parts.joinToString(" ")
    }

    private fun convertIndianNumberTamil(n: Long): String {
        if (n == 0L) return "பூஜ்ஜியம்"
        val parts = mutableListOf<String>()

        val crore = n / 10_000_000L
        var rem = n % 10_000_000L
        if (crore > 0) parts.add("${convertIndianNumberTamil(crore)} கோடி")

        val lakh = rem / 100_000L
        rem %= 100_000L
        if (lakh > 0) parts.add("${convertTamilBelow100(lakh.toInt())} லட்சம்")

        val thousand = rem / 1000L
        rem %= 1000L
        if (thousand > 0) parts.add("${convertTamilBelow100(thousand.toInt())} ஆயிரம்")

        val hundred = rem / 100L
        rem %= 100L
        if (hundred > 0) parts.add("${convertTamilBelow100(hundred.toInt())} நூறு")

        if (rem > 0) parts.add(convertTamilBelow100(rem.toInt()))

        return parts.joinToString(" ")
    }

    private val taUnits = arrayOf("", "ஒன்று", "இரண்டு", "மூன்று", "நான்கு", "ஐந்து", "ஆறு", "ஏழு", "எட்டு", "ஒன்பது")
    private val taTeens = arrayOf("பத்து", "பதினொன்று", "பன்னிரண்டு", "பதிமூன்று", "பதினான்கு", "பதினைந்து", "பதினாறு", "பதினேழு", "பதினெட்டு", "பத்தொன்பது")
    private val taTens = arrayOf("", "பத்து", "இருபது", "முப்பது", "நாற்பது", "ஐம்பது", "அறுபது", "எழுபது", "எண்பது", "தொண்ணூறு")

    private fun convertTamilBelow100(n: Int): String {
        return when {
            n == 0 -> ""
            n < 10 -> taUnits[n]
            n < 20 -> taTeens[n - 10]
            n % 10 == 0 -> taTens[n / 10]
            else -> "${taTens[n / 10]} ${taUnits[n % 10]}"
        }
    }

    // --- Telugu ---
    private fun convertTelugu(rupees: Long, paise: Long): String {
        val parts = mutableListOf<String>()
        if (rupees > 0) parts.add("${convertIndianNumberTelugu(rupees)} రూపాయలు")
        if (paise > 0) parts.add("${convertTeluguBelow100(paise.toInt())} పైసలు")
        return parts.joinToString(" ")
    }

    private fun convertIndianNumberTelugu(n: Long): String {
        if (n == 0L) return "సున్నా"
        val parts = mutableListOf<String>()

        val crore = n / 10_000_000L
        var rem = n % 10_000_000L
        if (crore > 0) parts.add("${convertIndianNumberTelugu(crore)} కోటి")

        val lakh = rem / 100_000L
        rem %= 100_000L
        if (lakh > 0) parts.add("${convertTeluguBelow100(lakh.toInt())} లక్ష")

        val thousand = rem / 1000L
        rem %= 1000L
        if (thousand > 0) parts.add("${convertTeluguBelow100(thousand.toInt())} వేల")

        val hundred = rem / 100L
        rem %= 100L
        if (hundred > 0) parts.add("${convertTeluguBelow100(hundred.toInt())} వందల")

        if (rem > 0) parts.add(convertTeluguBelow100(rem.toInt()))

        return parts.joinToString(" ")
    }

    private val teUnits = arrayOf("", "ఒకటి", "రెండు", "మూడు", "నాలుగు", "ఐదు", "ఆరు", "ఏడు", "ఎనిమిది", "తొమ్మిది")
    private val teTeens = arrayOf("పది", "పదకొండు", "పన్నెండు", "పదమూడు", "పద్నాలుగు", "పదిహేను", "పదహారు", "పదిహేడు", "పద్దెనిమిది", "పంతొమ్మిది")
    private val teTens = arrayOf("", "పది", "ఇరవై", "ముప్పై", "నలభై", "యాభై", "అరవై", "డెబ్బై", "ఎనభై", "తొంభై")

    private fun convertTeluguBelow100(n: Int): String {
        return when {
            n == 0 -> ""
            n < 10 -> teUnits[n]
            n < 20 -> teTeens[n - 10]
            n % 10 == 0 -> teTens[n / 10]
            else -> "${teTens[n / 10]} ${teUnits[n % 10]}"
        }
    }

    // --- Kannada ---
    private fun convertKannada(rupees: Long, paise: Long): String {
        val parts = mutableListOf<String>()
        if (rupees > 0) parts.add("${convertIndianNumberKannada(rupees)} ರೂಪಾಯಿ")
        if (paise > 0) parts.add("${convertKannadaBelow100(paise.toInt())} ಪೈಸೆ")
        return parts.joinToString(" ")
    }

    private fun convertIndianNumberKannada(n: Long): String {
        if (n == 0L) return "ಶೂನ್ಯ"
        val parts = mutableListOf<String>()

        val crore = n / 10_000_000L
        var rem = n % 10_000_000L
        if (crore > 0) parts.add("${convertIndianNumberKannada(crore)} ಕೋಟಿ")

        val lakh = rem / 100_000L
        rem %= 100_000L
        if (lakh > 0) parts.add("${convertKannadaBelow100(lakh.toInt())} ಲಕ್ಷ")

        val thousand = rem / 1000L
        rem %= 1000L
        if (thousand > 0) parts.add("${convertKannadaBelow100(thousand.toInt())} ಸಾವಿರ")

        val hundred = rem / 100L
        rem %= 100L
        if (hundred > 0) parts.add("${convertKannadaBelow100(hundred.toInt())} ನೂರು")

        if (rem > 0) parts.add(convertKannadaBelow100(rem.toInt()))

        return parts.joinToString(" ")
    }

    private val knUnits = arrayOf("", "ಒಂದು", "ಎರಡು", "ಮೂರು", "ನಾಲ್ಕು", "ಐದು", "ಆರು", "ಏಳು", "ಎಂಟು", "ಒಂಬತ್ತು")
    private val knTeens = arrayOf("ಹತ್ತು", "ಹನ್ನೊಂದು", "ಹನ್ನೆರಡು", "ಹದಿಮೂರು", "ಹದಿನಾಲ್ಕು", "ಹದಿನೈದು", "ಹದಿನಾರು", "ಹದಿನೇಳು", "ಹದಿನೆಂಟು", "ಹತ್ತೊಂಬತ್ತು")
    private val knTens = arrayOf("", "ಹತ್ತು", "ಇಪ್ಪತ್ತು", "ಮೂವತ್ತು", "ನಲವತ್ತು", "ಐವತ್ತು", "ಅರವತ್ತು", "ಎಪ್ಪತ್ತು", "ಎಂಬತ್ತು", "ತೊಂಬತ್ತು")

    private fun convertKannadaBelow100(n: Int): String {
        return when {
            n == 0 -> ""
            n < 10 -> knUnits[n]
            n < 20 -> knTeens[n - 10]
            n % 10 == 0 -> knTens[n / 10]
            else -> "${knTens[n / 10]} ${knUnits[n % 10]}"
        }
    }
}

