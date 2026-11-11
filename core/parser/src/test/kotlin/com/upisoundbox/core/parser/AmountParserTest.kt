package com.upisoundbox.core.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AmountParserTest {
    private val parser = AmountParser()

    @Test
    fun parsesStandardRupeeSymbol() {
        val amounts = parser.findAllAmounts("Received ₹500 from Rahul")
        assertThat(amounts).hasSize(1)
        assertThat(amounts[0].amountPaise).isEqualTo(50000L)
    }

    @Test
    fun parsesRupeeWithPaise() {
        val amounts = parser.findAllAmounts("Received ₹1,250.75 via UPI")
        assertThat(amounts).hasSize(1)
        assertThat(amounts[0].amountPaise).isEqualTo(125075L)
    }

    @Test
    fun parsesRsPrefix() {
        val amounts = parser.findAllAmounts("Paid you Rs. 99.00 today")
        assertThat(amounts).hasSize(1)
        assertThat(amounts[0].amountPaise).isEqualTo(9900L)
    }

    @Test
    fun parsesRsWithoutDot() {
        val amounts = parser.findAllAmounts("Credited with Rs 15")
        assertThat(amounts).hasSize(1)
        assertThat(amounts[0].amountPaise).isEqualTo(1500L)
    }

    @Test
    fun parsesInrPrefix() {
        val amounts = parser.findAllAmounts("Account credited with INR 10,000.00")
        assertThat(amounts).hasSize(1)
        assertThat(amounts[0].amountPaise).isEqualTo(1000000L)
    }

    @Test
    fun parsesIndianLakhGrouping() {
        val amounts = parser.findAllAmounts("Payment of ₹1,50,000 received")
        assertThat(amounts).hasSize(1)
        assertThat(amounts[0].amountPaise).isEqualTo(15000000L)
    }

    @Test
    fun parsesIndianCroreGrouping() {
        val amounts = parser.findAllAmounts("Transfer of ₹2,50,00,000.50 credited")
        assertThat(amounts).hasSize(1)
        assertThat(amounts[0].amountPaise).isEqualTo(2500000050L)
    }

    @Test
    fun picksAmountClosestToCreditKeyword() {
        // Here 450 is near "received", while 12,000 is an account balance
        val text = "Received ₹450 from John. Total balance is ₹12,000.00"
        val kwMatcher = KeywordMatcher()
        val creditMatches = kwMatcher.findCreditKeywords(text)
        val creditPositions = creditMatches.map { (it.range.first + it.range.last) / 2 }

        val best = parser.findBestAmount(text, creditPositions)
        assertThat(best).isNotNull()
        assertThat(best!!.amountPaise).isEqualTo(45000L)
    }

    @Test
    fun formatsPaiseCorrectly() {
        assertThat(AmountParser.formatPaise(50000L)).isEqualTo("₹500")
        assertThat(AmountParser.formatPaise(50050L)).isEqualTo("₹500.50")
        assertThat(AmountParser.formatPaise(12345678L)).isEqualTo("₹1,23,456.78")
    }
}

