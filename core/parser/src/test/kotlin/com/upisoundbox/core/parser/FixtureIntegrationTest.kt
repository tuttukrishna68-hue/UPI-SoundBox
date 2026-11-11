package com.upisoundbox.core.parser

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Test

class FixtureIntegrationTest {
    private val router = ParserRouter()
    private val smsParser = SmsParser()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun verifyAllFixtures() {
        val fixtureFiles = listOf(
            "fixtures/gpay.json",
            "fixtures/phonepe.json",
            "fixtures/paytm.json",
            "fixtures/bhim.json",
            "fixtures/amazon_pay.json",
            "fixtures/cred.json",
            "fixtures/generic.json",
            "fixtures/sms.json",
            "fixtures/negatives.json"
        )

        var totalExecuted = 0

        for (file in fixtureFiles) {
            val content = javaClass.classLoader.getResourceAsStream(file)
                ?.bufferedReader()?.use { it.readText() }
                ?: error("Could not find fixture resource: $file")

            val array = json.parseToJsonElement(content).jsonArray

            for (element in array) {
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: "unknown"
                val shouldParse = obj["shouldParse"]?.jsonPrimitive?.booleanOrNull ?: true

                if (file == "fixtures/sms.json" || obj.containsKey("sender")) {
                    val sender = obj["sender"]?.jsonPrimitive?.content ?: "BANK"
                    val body = obj["body"]?.jsonPrimitive?.content ?: ""
                    val result = smsParser.parse(RawSms(sender, body))

                    if (shouldParse) {
                        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
                        val success = result as ParseResult.Success
                        val expectedAmount = obj["expectedAmountPaise"]?.jsonPrimitive?.longOrNull
                        if (expectedAmount != null) {
                            assertThat(success.event.amountPaise).isEqualTo(expectedAmount)
                        }
                    } else {
                        assertThat(result).isNotInstanceOf(ParseResult.Success::class.java)
                    }
                } else {
                    val pkg = obj["package"]?.jsonPrimitive?.content ?: "com.generic.app"
                    val title = obj["title"]?.jsonPrimitive?.content
                    val text = obj["text"]?.jsonPrimitive?.content
                    val isGroupSummary = obj["isGroupSummary"]?.jsonPrimitive?.booleanOrNull ?: false
                    val isOngoing = obj["isOngoing"]?.jsonPrimitive?.booleanOrNull ?: false

                    val notification = RawNotification(
                        key = id,
                        packageName = pkg,
                        postTime = System.currentTimeMillis(),
                        title = title,
                        text = text,
                        isGroupSummary = isGroupSummary,
                        isOngoing = isOngoing
                    )

                    val result = router.parse(notification)

                    if (shouldParse) {
                        assertWithMessage("Fixture $id in $file failed to parse successfully: $result")
                            .that(result)
                            .isInstanceOf(ParseResult.Success::class.java)

                        val success = result as ParseResult.Success
                        val expectedAmount = obj["expectedAmountPaise"]?.jsonPrimitive?.longOrNull
                        if (expectedAmount != null) {
                            assertThat(success.event.amountPaise).isEqualTo(expectedAmount)
                        }

                        val expectedPayer = obj["expectedPayer"]?.jsonPrimitive?.content
                        if (expectedPayer != null) {
                            assertThat(success.event.payer).isEqualTo(expectedPayer)
                        }

                        val expectedRef = obj["expectedUpiRef"]?.jsonPrimitive?.content
                        if (expectedRef != null) {
                            assertThat(success.event.upiRef).isEqualTo(expectedRef)
                        }
                    } else {
                        assertWithMessage("Fixture $id in $file was expected to be rejected but succeeded: $result")
                            .that(result)
                            .isNotInstanceOf(ParseResult.Success::class.java)
                    }
                }
                totalExecuted++
            }
        }

        println("Successfully executed $totalExecuted table-driven fixture cases!")
        assertThat(totalExecuted).isAtLeast(100)
    }

    private fun assertWithMessage(message: String) = com.google.common.truth.Truth.assertWithMessage(message)
}

