package com.upisoundbox.core.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NumberToWordsTest {

    @Test
    fun englishRupeesAndPaise() {
        assertThat(NumberToWords.convertPaise(50000L, "en-IN"))
            .isEqualTo("Five Hundred rupees")
        assertThat(NumberToWords.convertPaise(100L, "en-IN"))
            .isEqualTo("One rupee")
        assertThat(NumberToWords.convertPaise(50L, "en-IN"))
            .isEqualTo("Fifty paise")
        assertThat(NumberToWords.convertPaise(125075L, "en-IN"))
            .isEqualTo("One Thousand Two Hundred Fifty rupees and Seventy Five paise")
        assertThat(NumberToWords.convertPaise(10000000L, "en-IN"))
            .isEqualTo("One Lakh rupees")
        assertThat(NumberToWords.convertPaise(25000000000L, "en-IN"))
            .isEqualTo("Twenty Five Crore rupees")
    }

    @Test
    fun hindiNumbers() {
        assertThat(NumberToWords.convertPaise(50000L, "hi-IN"))
            .isEqualTo("पाँच सौ रुपये")
        assertThat(NumberToWords.convertPaise(10000000L, "hi-IN"))
            .isEqualTo("एक लाख रुपये")
        assertThat(NumberToWords.convertPaise(15050L, "hi-IN"))
            .isEqualTo("एक सौ पचास रुपये पचास पैसे")
    }

    @Test
    fun malayalamNumbers() {
        assertThat(NumberToWords.convertPaise(50000L, "ml-IN"))
            .isEqualTo("അഞ്ച് നൂറ് രൂപ")
        assertThat(NumberToWords.convertPaise(100000L, "ml-IN"))
            .isEqualTo("ഒരു ആയിരം രൂപ".trim()) // or 1000
        assertThat(NumberToWords.convertPaise(5000L, "ml-IN"))
            .contains("രൂപ")
    }

    @Test
    fun tamilNumbers() {
        assertThat(NumberToWords.convertPaise(50000L, "ta-IN"))
            .isEqualTo("ஐந்து நூறு ரூபாய்")
        assertThat(NumberToWords.convertPaise(10000000L, "ta-IN"))
            .isEqualTo("ஒன்று லட்சம் ரூபாய்")
    }

    @Test
    fun teluguNumbers() {
        assertThat(NumberToWords.convertPaise(50000L, "te-IN"))
            .isEqualTo("ఐదు వందల రూపాయలు")
        assertThat(NumberToWords.convertPaise(10000000L, "te-IN"))
            .isEqualTo("ఒకటి లక్ష రూపాయలు")
    }

    @Test
    fun kannadaNumbers() {
        assertThat(NumberToWords.convertPaise(50000L, "kn-IN"))
            .isEqualTo("ಐದು ನೂರು ರೂಪಾಯಿ")
        assertThat(NumberToWords.convertPaise(10000000L, "kn-IN"))
            .isEqualTo("ಒಂದು ಲಕ್ಷ ರೂಪಾಯಿ")
    }
}

