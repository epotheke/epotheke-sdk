package com.epotheke.cardlink.mock

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class DeserExampleTest {

    @Test
    fun testDeser() {
        Assertions.assertDoesNotThrow {
            getAvailablePrescriptionListsExample(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        }
    }
}
