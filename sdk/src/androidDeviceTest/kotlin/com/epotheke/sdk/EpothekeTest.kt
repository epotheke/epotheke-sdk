package com.epotheke.sdk

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.epotheke.cardlink.UserInteraction
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val logger = KotlinLogging.logger {}

@RunWith(AndroidJUnit4::class)
class EpothekeTest {
    // @Test
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

    val uiMock = mock<UserInteraction> { }

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
                    Service.DEV.url,
                    Service.DEV.tenantToken,
                )

            assertNotNull(epotheke.cardlinkAuthenticationProtocol.establishCardlink(uiMock))
            val prescriptions =
                assertNotNull(
                    epotheke.prescriptionProtocol.requestPrescriptions(),
                )

            assertTrue { prescriptions.availablePrescriptionLists.isNotEmpty() }
        }
}
