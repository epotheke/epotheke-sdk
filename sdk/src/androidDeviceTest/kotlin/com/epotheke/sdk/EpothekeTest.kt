package com.epotheke.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.epotheke.Epotheke
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.spy
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.websocket.WebSocketException
import org.junit.BeforeClass
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}

@RunWith(AndroidJUnit4::class)
class EpothekeTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun checkNfc() {
            ensureNFCOn()
        }
    }

    val uiMock = spy(userInterActionStub())

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testInvalidTokens() =
        runTestJobWithActivity { activity ->
            listOf(
                TENANT_TOKEN_INVALID_DEV,
                TENANT_TOKEN_EXPIRED_DEV,
                TENANT_TOKEN_REVOKED_DEV,
            ).forEach {
                assertIs<WebSocketException>(
                    assertFails {
                        Epotheke(
                            assertNotNull(activity.factory),
                            SERVICE_URL_DEV,
                            it,
                        ).use { epotheke ->
                            epotheke.prescriptionProtocol.requestPrescriptions()
                        }
                    }.cause,
                )
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testEpotheke() =
        runTestJobWithActivity { activity ->
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
            everySuspend { uiMock.onCanRequest() } returns CAN_CORRECT

            everySuspend { uiMock.requestCardInsertion() } calls {
                activity.msg("insert card")
            }
            everySuspend { uiMock.onCardRecognized() } calls {
                activity.msg("don't move")
            }

            Epotheke(
                assertNotNull(activity.factory),
                SERVICE_URL_DEV,
                null,
            ).use { epotheke ->
                assertNotNull(epotheke.cardLinkAuthenticationProtocol.establishCardLink(uiMock))
                val prescriptions =
                    assertNotNull(
                        epotheke.prescriptionProtocol.requestPrescriptions(),
                    )

                assertTrue { prescriptions.availablePrescriptionLists.isNotEmpty() }
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class, ExperimentalUuidApi::class)
    @Test
    fun testEpothekeTwoEgks() =
        runTestJobWithActivity { activity ->
            val wsSessionId = Uuid.random().toString()
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
            everySuspend { uiMock.onCanRequest() } returns CAN_CORRECT

            var first = true
            everySuspend { uiMock.requestCardInsertion() } calls {
                if (first) {
                    activity.msg("insert card")
                    first = false
                } else {
                    activity.msg("insert different card")
                }
            }
            everySuspend { uiMock.onCardRecognized() } calls {
                activity.msg("don't move")
            }

            Epotheke(
                assertNotNull(activity.factory),
                SERVICE_URL_DEV,
                null,
                wsSessionId,
            ).use { epotheke ->
                assertNotNull(epotheke.cardLinkAuthenticationProtocol.establishCardLink(uiMock))
                assertNotNull(epotheke.cardLinkAuthenticationProtocol.establishCardLink(uiMock))
                val prescriptions =
                    assertNotNull(
                        epotheke.prescriptionProtocol.requestPrescriptions(),
                    )

                assertEquals(2, prescriptions.availablePrescriptionLists.size)
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class, ExperimentalUuidApi::class)
    @Test
    fun testSecondCardHasNoSMSTAN() =
        runTestJobWithActivity { activity ->
            val wsSessionId = Uuid.random().toString()
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
            everySuspend { uiMock.onCanRequest() } returns CAN_CORRECT

            everySuspend { uiMock.requestCardInsertion() } calls {
                activity.msg("insert card")
            }
            everySuspend { uiMock.onCardRecognized() } calls {
                activity.msg("don't move")
            }

            Epotheke(
                assertNotNull(activity.factory),
                SERVICE_URL_DEV,
                null,
                wsSessionId,
            ).use { epotheke ->

                assertNotNull(epotheke.cardLinkAuthenticationProtocol.establishCardLink(uiMock))
                assertNotNull(epotheke.cardLinkAuthenticationProtocol.establishCardLink(uiMock))

                verifySuspend(exactly(1)) {
                    uiMock.onPhoneNumberRequest()
                }
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun getPrescriptionsForOldSession() =
        runTestJobWithActivity { activity ->
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
            everySuspend { uiMock.onCanRequest() } returns CAN_CORRECT

            everySuspend { uiMock.requestCardInsertion() } calls {
                activity.msg("insert card")
            }
            everySuspend { uiMock.onCardRecognized() } calls {
                activity.msg("don't move")
            }

            Epotheke(
                assertNotNull(activity.factory),
                SERVICE_URL_DEV,
                null,
                null,
            ).use { epotheke ->

                val res = assertNotNull(epotheke.cardLinkAuthenticationProtocol.establishCardLink(uiMock))

                // new instance and connection with session id from last result
                val epothekeNew =
                    Epotheke(
                        assertNotNull(activity.factory),
                        SERVICE_URL_DEV,
                        null,
                        res.wsSessionId,
                    )

                val prescriptions =
                    assertNotNull(
                        epothekeNew.prescriptionProtocol.requestPrescriptions(),
                    )

                assertEquals(1, prescriptions.availablePrescriptionLists.size)
            }
        }
}
