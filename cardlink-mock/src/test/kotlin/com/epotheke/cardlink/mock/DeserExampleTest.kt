package com.epotheke.cardlink.mock

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@QuarkusTest
class DeserExampleTest {

    @Test
    fun testDeser() {
        Assertions.assertDoesNotThrow {
            getAvailablePrescriptionListsExample()
        }
    }
}
