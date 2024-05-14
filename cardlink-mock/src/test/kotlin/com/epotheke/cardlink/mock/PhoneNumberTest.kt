package com.epotheke.cardlink.mock

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


@QuarkusTest
class PhoneNumberTest {

    @Inject
    lateinit var smsSender: SpryngsmsSender

    @ParameterizedTest
    @MethodSource("germanPhoneNumberProvider")
    fun testGermanNumbers(phoneNumber: String) {
        val isGermanNumber = smsSender.isGermanPhoneNumber(phoneNumber)
        Assertions.assertTrue(isGermanNumber)
    }

    @ParameterizedTest
    @MethodSource("nonGermanPhoneNumberProvider")
    fun testNonGermanNumbers(phoneNumber: String) {
        val isGermanNumber = smsSender.isGermanPhoneNumber(phoneNumber)
        Assertions.assertFalse(isGermanNumber)
    }

    companion object {
        @JvmStatic
        fun germanPhoneNumberProvider(): Stream<String> {
            return Stream.of(
                "+49 15172612345",
                "015172612345",
                "01736322621",
            )
        }
        @JvmStatic
        fun nonGermanPhoneNumberProvider(): Stream<String> {
            return Stream.of(
                "+41 44 668 18 00",
                "+40 12 638 68 00"
            )
        }
    }
}
