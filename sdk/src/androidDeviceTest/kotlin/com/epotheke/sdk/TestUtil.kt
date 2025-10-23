package com.epotheke.sdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.launchActivity
import com.epotheke.cardlink.CardCommunicationResultCode
import com.epotheke.cardlink.ResultCode
import com.epotheke.cardlink.UserInteraction
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openecard.sc.pcsc.AndroidTerminalFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

const val SERVICE_URL_DEV = "https://service.dev.epotheke.com/cardlink"
const val SERVICE_URL_PROD = "https://service.epotheke.com/cardlink"
const val SERVICE_URL_MOCK = "https://mock.test.epotheke.com/cardlink"

@Suppress("ktlint:standard:max-line-length")
const val TENANT_TOKEN_VALID_DEV = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMjEyODQ3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzI4NDg0NywianRpIjoiZGQyN2ZhYmQtMGNmNC00MGVkLThkNjQtMGUzNzlmZWRiMDhiIn0.xD2KqPFaLaXCDm0PO2nvhNFLOxsOqgTq1Np9PqQCmho3StAMjrrp6W1PWQbbxgtCFBY_g5j6y7eKhAx7oUpX0g"

@Suppress("ktlint:standard:max-line-length")
const val TENANT_TOKEN_VALID_PROD = "eyJraWQiOiJ0ZW5hbnQtc2lnbmVyLTIwMjQxMTA2IiwiYWxnIjoiRVMyNTYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiMDE5M2NlZTMtMTdkOC03MDAwLTkwOTktZmM4NGNlMjYyNzk1IiwiaWF0IjoxNzQxMTczNzM4LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTgwNDI0NTczOCwianRpIjoiYWE2NDA5NWMtY2NlNy00N2FjLWEzZDItYzA2ZThlYjE2MmVmIn0.L0D7XGchxtkv_rzvzvru6t80MJy8aQKhbiTReH69MNBVgp9Z-wUlDgIPdpbySmhDSTVEbp1rCwQAOyXje1dntQ"

@Suppress("ktlint:standard:max-line-length")
const val TENANT_TOKEN_INVALID_DEV = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI7IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMjEyODQ3LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzI4NDg0NywianRpIjoiZGQyN2ZhYmQtMGNmNC00MGVkLThkNjQtMGUzNzlmZWRiMDhiIn0.xD2KqPFaLaXCDm0PO2nvhNFLOxsOqgTq1Np9PqQCmho3StAMjrrp6W1PWQbbxgtCFBY_g5j6y7eKhAx7oUpX0g"

@Suppress("ktlint:standard:max-line-length")
const val TENANT_TOKEN_REVOKED_DEV = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMzY1NzgxLCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTc5MzQzNzc4MSwianRpIjoiYTkxNGQxMGItYmI0NS00NDcyLTg0NWUtYzZiNTNiOTNiNjhmIn0.en-cBlvd5jO0Nz2kuj7dPNFH5xlzPd9TLQZLjxdBkiSfRlV9-i060zO3emUhN8tgSU5ZmwlcGF1sRJLbwJSyPg"

@Suppress("ktlint:standard:max-line-length")
const val TENANT_TOKEN_EXPIRED_DEV = "eyJraWQiOiJ0ZXN0LXRlbmFudC1zaWduZXItMjAyNDEwMDgiLCJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZXJ2aWNlLmVwb3RoZWtlLmNvbSIsImF1ZCI6InNlcnZpY2UuZXBvdGhla2UuY29tIiwic3ViIjoiYzcyNGFkMTktZmJmYy00MmFlLThlZDYtN2IzMDgxNDIyNzI5IiwiaWF0IjoxNzMwMzY2MDc5LCJncm91cHMiOlsidGVuYW50Il0sImV4cCI6MTczMDM2NjM3OSwianRpIjoiNTk3MDFkMTktMjEwNC00OGI0LWI2ZDQtOWQ0ZDhmNmIxZmVjIn0.9Wqj4YMAV18Lfm6v5SdcI8dlGAuqA8TsAuTyDXt5IBKEZaI1OWBq_RdxwP78nD_9H3eX8VgL_9EJ5VpvEWyn4g"

const val PHONE_NUMBER_VALID = "+49 151 000 000 00"
const val PHONE_NUMBER_INVALID = "42"
const val PHONE_NUMBER_WRONG_COUNTRY = "+39 312 123 123 3"
const val PHONE_NUMBER_BLOCKED = "+49 15172612342"

const val TAN_CORRECT = "123123"
const val TAN_INCORRECT = "666666"
const val TAN_INVALID = "AAA"

const val CAN_CORRECT = "123123"
const val CAN_WRONG = "666666"

private val logger = KotlinLogging.logger { }

class TestActivity : Activity() {
    var textView: TextView? = null
    var factory: AndroidTerminalFactory? = null

    var txtField: EditText? = null
    var okBtn: Button? = null

    var wasPaused = false
    var wasResumedAfterPaused = false
    var ignoreResumeByNewIntent = false

    fun msg(msg: String) =
        runOnUiThread {
            textView?.text = msg
        }

    override fun onPause() {
        super.onPause()
        wasPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (wasPaused && !ignoreResumeByNewIntent) {
            wasResumedAfterPaused = true
        }
        ignoreResumeByNewIntent = false
    }

    suspend fun getPhoneNumber(msg: String = "Please provide PhoneNumber"): String {
        msg(msg)
        return getString()
    }

    suspend fun getTan(msg: String = "Please provide TAN"): String {
        msg(msg)
        return getString()
    }

    suspend fun getCan(msg: String = "Please provide CAN"): String {
        msg(msg)
        return getString()
    }

    suspend fun getString(): String {
        val result = CompletableDeferred<String>()
        runOnUiThread {
            txtField?.isEnabled = true
            okBtn?.isEnabled = true
            okBtn?.setOnClickListener {
                result.complete(txtField?.text.toString())
                txtField?.text?.clear()
                txtField?.isEnabled = false
                okBtn?.isEnabled = false
            }
        }
        return result.await()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        factory = AndroidTerminalFactory.instance(this)
        txtField =
            EditText(this).apply {
                isEnabled = false
                text = SpannableStringBuilder("123123")
                inputType = InputType.TYPE_CLASS_TEXT
                gravity = Gravity.CENTER
            }
        textView =
            TextView(this@TestActivity).apply {
                text = "Running tests. - Nothing to do for you now - stay tuned."
                textSize = 24f
                gravity = Gravity.CENTER
            }
        okBtn =
            Button(this).apply {
                isEnabled = false
                text = "OK"
                textSize = 24f
                gravity = Gravity.CENTER
            }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(
                    textView,
                    FrameLayout
                        .LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                        ).apply { gravity = Gravity.CENTER },
                )
                addView(txtField)
                addView(okBtn)
            },
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ignoreResumeByNewIntent = true
        intent?.let {
            factory?.tagIntentHandler(it)
        }
    }
}

fun userInterActionStub() =
    object : UserInteraction {
        override suspend fun requestCardInsertion() {
            logger.debug { "requestCardInsertion" }
        }

        override suspend fun onCardRecognized() {
            logger.debug { "onCardRecognized" }
        }

        override suspend fun onCanRequest(): String {
            logger.debug { "onCanRequest" }
            throw NotImplementedError("Methods returning must be mocked.")
        }

        override suspend fun onCanRetry(resultCode: CardCommunicationResultCode): String {
            logger.debug { "onCanRetry" }
            throw NotImplementedError("onCanRetry not mocked - Methods returning must be mocked.")
        }

        override suspend fun onPhoneNumberRequest(): String {
            logger.debug { "onPhoneNumberRequest" }
            throw NotImplementedError("onPhoneNumberRequest not mocked - Methods returning must be mocked.")
        }

        override suspend fun onPhoneNumberRetry(
            resultCode: ResultCode,
            msg: String?,
        ): String {
            logger.debug { "onPhoneNumberRetry" }
            throw NotImplementedError("onPhoneNumberRetry not mocked - Methods returning must be mocked.")
        }

        override suspend fun onTanRequest(): String {
            logger.debug { "onTanRequest" }
            throw NotImplementedError("onTanRequest not mocked - Methods returning must be mocked.")
        }

        override suspend fun onTanRetry(
            resultCode: ResultCode,
            msg: String?,
        ): String {
            logger.debug { "onTanRetry" }
            throw NotImplementedError("onTanRetry not mocked - Methods returning must be mocked.")
        }
    }

private suspend fun countDown(
    activity: TestActivity,
    testInstructions: String,
    timeout: Duration = 10.seconds,
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

fun runTestJobWithActivity(testJob: suspend CoroutineScope.(activity: TestActivity) -> Unit) {
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
