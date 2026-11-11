package com.upisoundbox.core.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PayerParserTest {
    private val parser = PayerParser()

    @Test
    fun extractsAndMasksVpa() {
        val result = parser.extract("Received ₹200 from rahul123@oksbi via UPI")
        assertThat(result.payer).isEqualTo("rah****3@oksbi")
    }

    @Test
    fun masksShortVpaHandlesCorrectly() {
        assertThat(PayerParser.maskVpa("ab@upi")).isEqualTo("ab****@upi")
        assertThat(PayerParser.maskVpa("abc@upi")).isEqualTo("ab****@upi")
        assertThat(PayerParser.maskVpa("john@ybl")).isEqualTo("joh****n@ybl")
        assertThat(PayerParser.maskVpa("rameshkumar@icici")).isEqualTo("ram****r@icici")
    }

    @Test
    fun extractsPayerNameFromContext() {
        val result = parser.extract("₹500 received from Suresh Kumar via PhonePe")
        assertThat(result.payer).isEqualTo("Suresh Kumar")
    }

    @Test
    fun extracts12DigitUpiRef() {
        val result1 = parser.extract("Payment received. UPI Ref: 412345678901")
        assertThat(result1.upiRef).isEqualTo("412345678901")

        val result2 = parser.extract("Money credited. RRN: 499112233445")
        assertThat(result2.upiRef).isEqualTo("499112233445")
    }

    @Test
    fun ignoresNoiseWordsAsPayer() {
        val result = parser.extract("Payment received from Account")
        assertThat(result.payer).isNull()
    }
}

