package com.epotheke.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.epotheke.Epotheke.Companion.createEpothekeService
import com.epotheke.cardlink.SmartcardSalHelper
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.matcher.capture.get
import dev.mokkery.spy
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.websocket.WebSocketException
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.openecard.sal.sc.SmartcardSalSession
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
                        createEpothekeService(
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
            val salSessionCap = Capture.slot<SmartcardSalSession>()
            everySuspend { uiMock.requestCardInsertion(capture(salSessionCap)) } calls {
                activity.msg("Insert Card")
                val connection =
                    SmartcardSalHelper.connectFirstTerminalOnInsertCard(salSessionCap.get())

                activity.msg("Card connected - don't move.")
                connection
            }
            createEpothekeService(
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
            val salSessionCap = Capture.slot<SmartcardSalSession>()
            everySuspend { uiMock.requestCardInsertion(capture(salSessionCap)) } calls {
                if (first) {
                    first = false
                    activity.msg("insert card")
                } else {
                    activity.msg("insert different card")
                }
                val connection =
                    SmartcardSalHelper.connectFirstTerminalOnInsertCard(salSessionCap.get())

                activity.msg("Card connected - don't move.")
                connection
            }
            createEpothekeService(
                assertNotNull(activity.factory),
                SERVICE_URL_DEV,
                null,
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

            val salSessionCap = Capture.slot<SmartcardSalSession>()
            everySuspend { uiMock.requestCardInsertion(capture(salSessionCap)) } calls {
                activity.msg("Insert Card")
                val connection =
                    SmartcardSalHelper.connectFirstTerminalOnInsertCard(salSessionCap.get())

                activity.msg("Card connected - don't move.")
                connection
            }
            createEpothekeService(
                assertNotNull(activity.factory),
                SERVICE_URL_DEV,
                null,
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
            val salSessionCap = Capture.slot<SmartcardSalSession>()
            everySuspend { uiMock.requestCardInsertion(capture(salSessionCap)) } calls {
                activity.msg("Insert Card")
                val connection =
                    SmartcardSalHelper.connectFirstTerminalOnInsertCard(salSessionCap.get())

                activity.msg("Card connected - don't move.")
                connection
            }
            createEpothekeService(
                assertNotNull(activity.factory),
                SERVICE_URL_DEV,
                null,
            ).use { epotheke ->

                val res = assertNotNull(epotheke.cardLinkAuthenticationProtocol.establishCardLink(uiMock))

                // new instance and connection with session id from last result
                val epothekeNew =
                    createEpothekeService(
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
