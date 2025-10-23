package com.epotheke.sdk

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.epotheke.Epotheke
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.spy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}

@RunWith(AndroidJUnit4::class)
class EpothekeTest {
    @BeforeAll
    fun assureNfcOn() {
        runBlocking {
            launchActivity<TestActivity>().use {
                it.onActivity { activity ->
                    activity.factory?.load()
                    assert(activity.factory?.nfcAvailable == true) {
                        "NFC not available"
                    }
                    assert(activity.factory?.nfcEnabled == true) {
                        "NFC not enabled"
                    }
                }
            }
        }
    }

    val uiMock = spy(userInterActionStub())

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

            val epotheke =
                Epotheke(
                    assertNotNull(activity.factory),
                    SERVICE_URL_DEV,
                    TENANT_TOKEN_VALID_DEV,
                )

            assertNotNull(epotheke.cardlinkAuthenticationProtocol.establishCardlink(uiMock))
            val prescriptions =
                assertNotNull(
                    epotheke.prescriptionProtocol.requestPrescriptions(),
                )

            assertTrue { prescriptions.availablePrescriptionLists.isNotEmpty() }
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

            val epotheke =
                Epotheke(
                    assertNotNull(activity.factory),
                    SERVICE_URL_DEV,
                    TENANT_TOKEN_VALID_DEV,
                    wsSessionId,
                )

            assertNotNull(epotheke.cardlinkAuthenticationProtocol.establishCardlink(uiMock))
            assertNotNull(epotheke.cardlinkAuthenticationProtocol.establishCardlink(uiMock))
            val prescriptions =
                assertNotNull(
                    epotheke.prescriptionProtocol.requestPrescriptions(),
                )

            assertEquals(2, prescriptions.availablePrescriptionLists.size)
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

            val epotheke =
                Epotheke(
                    assertNotNull(activity.factory),
                    SERVICE_URL_DEV,
                    TENANT_TOKEN_VALID_DEV,
                    null,
                )

            val res = assertNotNull(epotheke.cardlinkAuthenticationProtocol.establishCardlink(uiMock))

            // new instance and connection with session id from last result
            val epothekeNew =
                Epotheke(
                    assertNotNull(activity.factory),
                    SERVICE_URL_DEV,
                    TENANT_TOKEN_VALID_DEV,
                    res.wsSessionId,
                )

            val prescriptions =
                assertNotNull(
                    epothekeNew.prescriptionProtocol.requestPrescriptions(),
                )

            assertEquals(1, prescriptions.availablePrescriptionLists.size)
        }
}
