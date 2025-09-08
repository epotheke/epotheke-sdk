package com.epotheke.sdk

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.epotheke.cardlink.CardLinkErrorCodes
import com.epotheke.cardlink.CardlinkAuthResult
import com.epotheke.cardlink.CardlinkAuthenticationConfig
import com.epotheke.cardlink.CardlinkAuthenticationException
import com.epotheke.cardlink.CardlinkAuthenticationProtocol
import com.epotheke.cardlink.ResultCode
import com.epotheke.cardlink.UserInteraction
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentially
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.matcher.matching
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.openecard.sc.pcsc.AndroidTerminalFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val logger = KotlinLogging.logger {}

@RunWith(AndroidJUnit4::class)
class NfcTest {
    private val testTimeout = 10.seconds

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

    private suspend fun countDown(
        activity: TestActivity,
        testInstructions: String,
        timeout: Duration = testTimeout,
        checkInBetween: () -> Boolean = { true },
        whenOver: suspend () -> Unit = {},
    ) {
        for (i in timeout.toInt(DurationUnit.SECONDS) downTo 1) {
            activity.msg(
                testInstructions +
                    "\n\n $i secs left",
            )
            if (!checkInBetween()) {
                break
            }
            delay(1.seconds)
        }
        whenOver()
    }

    private fun runTestJobWithActivity(testJob: suspend CoroutineScope.(activity: TestActivity) -> Unit) {
        runBlocking(Dispatchers.IO) {
            launchActivity<TestActivity>().use { scenario ->
                var j: Job? = null
                scenario.onActivity { activity ->
                    j =
                        launch {
                            testJob(activity)
                        }
                }
                j?.join()
            }
        }
    }

    val uiMock = mock<UserInteraction> { }

    private suspend fun callEstablishCardLink(
        activity: TestActivity,
        service: Service,
    ): CardlinkAuthResult? {
        val ws =
            WebsocketCommon(
                service.url,
                service.tenantToken,
            )

        val proto =
            CardlinkAuthenticationProtocol(
                assertNotNull(activity.factory),
                ws,
            )

        return proto.establishCardlink(uiMock)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testPhoneNumberHandling() =
        runTestJobWithActivity { activity ->
            var testJob: Job? = null
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } calls {
                // we just stop the test here
                testJob?.cancelAndJoin()
                "123123"
            }
            testJob =
                launch {
                    callEstablishCardLink(activity, Service.DEV)
                }

            testJob.join()
            verifySuspend {
                uiMock.onPhoneNumberRequest()
            }
            verifySuspend {
                uiMock.onTanRequest()
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testPhoneNumberHandlingWithInvalidData() =
        runTestJobWithActivity { activity ->
            val nbrWrongCountry = 2
            val nbrInvalidNumber = 2
            var testJob: Job? = null
            everySuspend { uiMock.onPhoneNumberRequest() } calls {
                PHONE_NUMBER_INVALID
            }
            everySuspend { uiMock.onPhoneNumberRetry(any(), any()) } sequentially {
                repeat(nbrWrongCountry) {
                    returns(PHONE_NUMBER_WRONG_COUNTRY)
                }
                repeat(nbrInvalidNumber - 1) {
                    returns(PHONE_NUMBER_INVALID)
                }
                returns(PHONE_NUMBER_VALID)
            }
            everySuspend { uiMock.onTanRequest() } calls {
                // we just stop the test here
                testJob?.cancel()
                "123123"
            }
            testJob =
                launch {
                    callEstablishCardLink(activity, Service.DEV)
                }

            testJob.join()
            verifySuspend {
                uiMock.onPhoneNumberRequest()
            }
            verifySuspend(exactly(nbrInvalidNumber)) {
                uiMock.onPhoneNumberRetry(
                    eq(
                        ResultCode.INVALID_REQUEST,
                    ),
                    matching {
                        it?.isNotEmpty() == true
                    },
                )
            }
            verifySuspend(exactly(nbrWrongCountry)) {
                uiMock.onPhoneNumberRetry(
                    eq(ResultCode.NUMBER_FROM_WRONG_COUNTRY),
                    matching {
                        it?.isNotEmpty() == true
                    },
                )
            }
            verifySuspend { uiMock.onTanRequest() }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testPhoneNumberBlocked() =
        runTestJobWithActivity { activity ->
            var testJob: Job? = null
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_BLOCKED
            everySuspend { uiMock.onPhoneNumberRetry(any(), any()) } calls {
                testJob?.cancelAndJoin()
                PHONE_NUMBER_BLOCKED
            }
            testJob =
                launch {
                    callEstablishCardLink(activity, Service.MOCK)
                }
            testJob.join()

            verifySuspend {
                uiMock.onPhoneNumberRetry(
                    eq(ResultCode.NUMBER_BLOCKED),
                    any(),
                )
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testTanHandling() =
        runTestJobWithActivity { activity ->
            var testJob: Job? = null
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
            everySuspend { uiMock.onCanRequest() } calls {
                // we just stop the test here
                testJob?.cancelAndJoin()
                "123123"
            }

            testJob =
                launch {
                    callEstablishCardLink(activity, Service.MOCK)
                }
            testJob.join()
            verifySuspend {
                uiMock.onPhoneNumberRequest()
            }
            verifySuspend {
                uiMock.onTanRequest()
            }
            verifySuspend {
                uiMock.onCanRequest()
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testTanHandlingRetryExceed() =
        runTestJobWithActivity { activity ->
            var testJob: Job? = null
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } returns TAN_INVALID
            everySuspend { uiMock.onTanRetry(any(), any()) } returns TAN_INCORRECT

            testJob =
                launch {
                    val e =
                        assertThrows<CardlinkAuthenticationException> {
                            callEstablishCardLink(activity, Service.MOCK)
                        }
                    assertEquals(CardLinkErrorCodes.CardLinkCodes.TAN_RETRY_LIMIT_EXCEEDED, e.code)
                }
            testJob.join()

            verifySuspend {
                uiMock.onTanRequest()
            }
            verifySuspend(exactly(3)) {
                uiMock.onTanRetry(eq(ResultCode.TAN_INCORRECT), matching { it?.isNotEmpty() ?: false })
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testCanSanitizing() =

        runTestJobWithActivity { activity ->
            val nbrWrongButValidTan = 3
            var testJob: Job? = null
            everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
            everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT

            everySuspend { uiMock.onCanRequest() } returns ""
            everySuspend { uiMock.onCanRetry(any(), any()) } sequentially {
                returns("1234567")
                returns("ABCDEF")
                repeat(nbrWrongButValidTan) {
                    returns(CAN_WRONG)
                }
                returns(CAN_CORRECT)
            }

            everySuspend { uiMock.requestCardInsertion() } calls {
                activity.msg("Bring card to device.")
            }
            everySuspend { uiMock.onCardRecognized() } calls {
                activity.msg("found card - don't move.")
            }
            testJob =
                launch {
                    callEstablishCardLink(activity, Service.DEV)
                }
            testJob.join()
            verifySuspend {
                uiMock.onPhoneNumberRequest()
            }
            verifySuspend {
                uiMock.onTanRequest()
            }
            verifySuspend {
                uiMock.onCanRequest()
            }
            verifySuspend {
                uiMock.onCanRetry(eq(CardLinkErrorCodes.ClientCodes.CAN_EMPTY), matching { it != null })
            }
            verifySuspend {
                uiMock.onCanRetry(eq(CardLinkErrorCodes.ClientCodes.CAN_TOO_LONG), matching { it != null })
            }
            verifySuspend {
                uiMock.onCanRetry(eq(CardLinkErrorCodes.ClientCodes.CAN_NOT_NUMERIC), matching { it != null })
            }
            verifySuspend(exactly(nbrWrongButValidTan)) {
                uiMock.onCanRetry(eq(CardLinkErrorCodes.ClientCodes.CAN_INCORRECT), matching { it != null })
            }
            verifySuspend { uiMock.requestCardInsertion() }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testReestablishCardConnectionAfterWrongCAN() =
        runTestJobWithActivity { activity ->
            launch {
                everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
                everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT

                everySuspend { uiMock.onCanRequest() } returns CAN_WRONG

                everySuspend { uiMock.onCanRetry(any(), any()) } calls {
                    countDown(activity, "Remove card", 5.seconds)
                    CAN_CORRECT
                }
                everySuspend { uiMock.onCardRemoved() } calls {
                    activity.msg("Card removed")
                }
                everySuspend { uiMock.requestCardInsertion() } calls {
                    activity.msg("Insert Card")
                }
                everySuspend { uiMock.onCardRecognized() } calls {
                    activity.msg("Card detected. Don't move")
                }

                val result =
                    assertNotNull(callEstablishCardLink(activity, Service.MOCK), "cardlink was not established.")

                assertNotNull(result.iccsn)
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testCardLinkReadPersonalData() =
        runTestJobWithActivity { activity ->
            launch {
                everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
                everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
                everySuspend { uiMock.onCanRequest() } returns CAN_CORRECT
                everySuspend { uiMock.requestCardInsertion() } calls {
                    activity.msg("Insert Card")
                }
                everySuspend { uiMock.onCardRecognized() } calls {
                    activity.msg("Card detected. Don't move")
                }

                CardlinkAuthenticationConfig.readPersonalData = true
                val result =
                    assertNotNull(callEstablishCardLink(activity, Service.MOCK), "cardlink was not established.")
                assertNotNull(result.personalData)
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testCardLinkReadInsurerData() =
        runTestJobWithActivity { activity ->
            launch {
                everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
                everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
                everySuspend { uiMock.onCanRequest() } returns CAN_CORRECT
                everySuspend { uiMock.requestCardInsertion() } calls {
                    activity.msg("Insert Card")
                }
                everySuspend { uiMock.onCardRecognized() } calls {
                    activity.msg("Card detected. Don't move")
                }

                CardlinkAuthenticationConfig.readInsurerData = true
                val result =
                    assertNotNull(callEstablishCardLink(activity, Service.MOCK), "cardlink was not established.")
                assertNotNull(result.insurerData)
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testCardLink_dev() =
        runTestJobWithActivity { activity ->
            launch {
                everySuspend { uiMock.onPhoneNumberRequest() } returns PHONE_NUMBER_VALID
                everySuspend { uiMock.onTanRequest() } returns TAN_CORRECT
                everySuspend { uiMock.onCanRequest() } returns CAN_CORRECT
                everySuspend { uiMock.requestCardInsertion() } calls {
                    activity.msg("Insert Card")
                }
                everySuspend { uiMock.onCardRecognized() } calls {
                    activity.msg("Card detected. Don't move")
                }

                val result =
                    assertNotNull(callEstablishCardLink(activity, Service.DEV), "cardlink was not established.")
                assertNotNull(result.iccsn)
            }
        }

    suspend fun advicePauseResume(activity: TestActivity) {
        activity.wasResumedAfterPaused = false
        countDown(
            activity,
            "Please suspend and reactivate app",
            10.seconds,
            { !activity.wasResumedAfterPaused },
            { assertTrue("Activitiy was not paused") { activity.wasResumedAfterPaused } },
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testSuspendDuringAuth() =
        runTestJobWithActivity { activity ->
            launch {
                everySuspend { uiMock.onPhoneNumberRequest() } calls {
                    advicePauseResume(activity)
                    PHONE_NUMBER_VALID
                }
                everySuspend { uiMock.onTanRequest() } calls {
                    advicePauseResume(activity)
                    TAN_CORRECT
                }
                everySuspend { uiMock.onCanRequest() } calls {
                    advicePauseResume(activity)
                    CAN_CORRECT
                }
                everySuspend { uiMock.requestCardInsertion() } calls {
                    activity.msg("Insert Card")
                }
                everySuspend { uiMock.onCardRecognized() } calls {
                    activity.msg("Card detected. Don't move")
                }

                val result =
                    assertNotNull(callEstablishCardLink(activity, Service.DEV), "cardlink was not established.")
                assertNotNull(result.iccsn)
            }
        }

    // @Test
    @OptIn(ExperimentalUnsignedTypes::class)
    fun testCardLink_prod() =
        runTestJobWithActivity { activity ->
            launch {
                everySuspend { uiMock.onPhoneNumberRequest() } calls { activity.getPhoneNumber() }
                everySuspend { uiMock.onTanRequest() } calls { activity.getTan() }
                everySuspend { uiMock.onCanRequest() } calls { activity.getCan() }
                everySuspend { uiMock.requestCardInsertion() } calls {
                    activity.msg("Insert Card")
                }
                everySuspend { uiMock.onCardRecognized() } calls {
                    activity.msg("Card detected. Don't move")
                }

                val result =
                    assertNotNull(callEstablishCardLink(activity, Service.PROD), "cardlink was not established.")
                assertNotNull(result.iccsn)
            }
        }
}
