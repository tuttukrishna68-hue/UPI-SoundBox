package com.upisoundbox.core.tts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnnouncementEngineTest {

    // Mock-free unit tests for phrase rendering across Indian languages
    @Test
    fun renderPhrase_english_replacesTokensCorrectly() {
        val template = "Received {amount} on {app} from {payer}"
        val rendered = renderPhraseHelper(
            template = template,
            amountPaise = 50000L,
            payer = "Rahul Sharma",
            appPackage = "com.phonepe.app",
            lang = "en-IN"
        )
        assertThat(rendered).isEqualTo("Received Five Hundred rupees on PhonePe from Rahul Sharma")
    }

    @Test
    fun renderPhrase_hindi_replacesTokensCorrectly() {
        val template = "{app} पर {payer} से {amount} प्राप्त हुए"
        val rendered = renderPhraseHelper(
            template = template,
            amountPaise = 50000L,
            payer = "Rahul Sharma",
            appPackage = "com.phonepe.app",
            lang = "hi-IN"
        )
        assertThat(rendered).isEqualTo("PhonePe पर Rahul Sharma से पाँच सौ रुपये प्राप्त हुए")
    }

    @Test
    fun renderPhrase_malayalam_replacesTokensCorrectly() {
        val template = "{app} വഴി {payer} ൽ നിന്ന് {amount} ലഭിച്ചു"
        val rendered = renderPhraseHelper(
            template = template,
            amountPaise = 50000L,
            payer = "Rahul Sharma",
            appPackage = "com.phonepe.app",
            lang = "ml-IN"
        )
        assertThat(rendered).contains("PhonePe")
        assertThat(rendered).contains("രൂപ ലഭിച്ചു")
    }

    @Test
    fun renderPhrase_tamil_replacesTokensCorrectly() {
        val template = "{app} மூலம் {payer} இடமிருந்து {amount} பெறப்பட்டது"
        val rendered = renderPhraseHelper(
            template = template,
            amountPaise = 50000L,
            payer = "Rahul Sharma",
            appPackage = "com.phonepe.app",
            lang = "ta-IN"
        )
        assertThat(rendered).contains("PhonePe")
        assertThat(rendered).contains("ரூபாய் பெறப்பட்டது")
    }

    @Test
    fun renderPhrase_telugu_replacesTokensCorrectly() {
        val template = "{app} ద్వారా {payer} నుండి {amount} అందింది"
        val rendered = renderPhraseHelper(
            template = template,
            amountPaise = 50000L,
            payer = "Rahul Sharma",
            appPackage = "com.phonepe.app",
            lang = "te-IN"
        )
        assertThat(rendered).contains("PhonePe")
        assertThat(rendered).contains("రూపాయలు అందింది")
    }

    @Test
    fun renderPhrase_kannada_replacesTokensCorrectly() {
        val template = "{app} ಮೂಲಕ {payer} ರಿಂದ {amount} ಸ್ವೀಕರಿಸಲಾಗಿದೆ"
        val rendered = renderPhraseHelper(
            template = template,
            amountPaise = 50000L,
            payer = "Rahul Sharma",
            appPackage = "com.phonepe.app",
            lang = "kn-IN"
        )
        assertThat(rendered).contains("PhonePe")
        assertThat(rendered).contains("ರೂಪಾಯಿ ಸ್ವೀಕರಿಸಲಾಗಿದೆ")
    }

    private fun renderPhraseHelper(
        template: String,
        amountPaise: Long,
        payer: String?,
        appPackage: String,
        lang: String
    ): String {
        val words = com.upisoundbox.core.parser.NumberToWords.convertPaise(amountPaise, lang)
        val app = AnnouncementEngine.mapPackageToDisplayName(appPackage)
        return template
            .replace("{amount}", words)
            .replace("{payer}", payer ?: "customer")
            .replace("{app}", app)
    }
}
