package com.demoreactnative

import android.app.Activity
import android.content.Intent
import com.epotheke.erezept.model.AvailablePrescriptionLists
import com.epotheke.erezept.model.RequestPrescriptionList
import com.epotheke.erezept.model.SelectedPrescriptionList
import com.epotheke.erezept.model.prescriptionJsonFormatter
import com.epotheke.sdk.CardLinkControllerCallback
import com.epotheke.sdk.CardLinkProtocol
import com.epotheke.sdk.PrescriptionProtocol
import com.epotheke.sdk.SdkCore
import com.epotheke.sdk.SdkErrorHandler
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.openecard.mobile.activation.ActivationResult
import org.openecard.mobile.activation.CardLinkInteraction
import org.openecard.mobile.activation.ConfirmPasswordOperation
import org.openecard.mobile.activation.ConfirmTextOperation
import org.openecard.mobile.activation.NFCOverlayMessageHandler
import org.openecard.mobile.activation.ServiceErrorResponse

private val logger = KotlinLogging.logger {}

class SdkModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "SdkModule"

    var onStartedCB: Callback? = null

    @ReactMethod
    fun set_controllerCallbackCB_onStarted(cb: Callback) {
        onStartedCB = cb
    }

    var onAuthenticationCompletionCB: Callback? = null

    @ReactMethod
    fun set_controllerCallbackCB_onAuthenticationCompletion(cb: Callback) {
        onAuthenticationCompletionCB = cb
    }

    private var erezeptProtocol: PrescriptionProtocol? = null
    private var ws_sessionId: String? = null
    private var lastICCSN: String? = null
    private var lastPersonalData: String? = null

    @ReactMethod
    fun getPrescriptions(filter: ReadableArray, p: Promise) {
        runBlocking {
            callPrescriptionProtocolNullChecked(p) {
                try {
                    val availablePrescriptions: AvailablePrescriptionLists =
                       requestPrescriptions(
                            iccsns = filter.toArrayList().mapNotNull { it as? String }
                       )
                    p.resolve(
                        prescriptionJsonFormatter.encodeToString(availablePrescriptions)
                    )
                } catch (e: Exception) {
                    p.reject(e)
                }
            }
        }
    }

    @ReactMethod
    fun selectPrescriptions(selection: String, p: Promise) {
        runBlocking {
            callPrescriptionProtocolNullChecked(p) {
                try {
                    val confirmation = selectPrescriptions(
                        prescriptionJsonFormatter.decodeFromString<SelectedPrescriptionList>(selection)
                    )
                    p.resolve(
                        prescriptionJsonFormatter.encodeToString(confirmation)
                    )
                } catch (e: Exception) {
                    p.reject(e)
                }
            }
        }
    }

    private suspend fun callPrescriptionProtocolNullChecked(p: Promise, call: suspend PrescriptionProtocol.() -> Unit) {
        when (val proto = erezeptProtocol) {
            null -> {
                p.reject("Protocol not available, is CardLink established?")
            }
            else -> proto.call()
        }
    }

    @ReactMethod
    fun getWsSessionId(p: Promise) {
        p.resolve(ws_sessionId)
    }
    @ReactMethod
    fun getLastICCSN(p: Promise) {
        p.resolve(lastICCSN)
    }

    @ReactMethod
    fun getLastPersonalData(p: Promise) {
        p.resolve(lastPersonalData)
    }

    private val cardLinkControllerCallback = object : CardLinkControllerCallback {
        override fun onAuthenticationCompletion(
            p0: ActivationResult?,
            cardLinkProtocols: Set<CardLinkProtocol>
        ) {
            logger.debug { "rn-bridge: onAuthenticationCompletion ${p0?.errorMessage}" }
            erezeptProtocol = cardLinkProtocols.filterIsInstance<PrescriptionProtocol>().first()


            //hotfix
            if (p0?.errorMessage?.contains("==>") == true) {
                var minor = p0.errorMessage?.split("==>")?.get(0)?.trim()
                val msg = p0.errorMessage?.split("==>")?.get(1)?.trim()
                if (minor?.contains("invalidSlotHandle") == true) {
                    minor = "CARD_REMOVED"
                }
                onAuthenticationCompletionCB?.invoke(minor, msg)
            } else if (p0?.errorMessage != null) {
                onAuthenticationCompletionCB?.invoke(p0.resultCode?.name, p0.errorMessage)
            } else {
                ws_sessionId = p0?.getResultParameter("CardLink::WS_SESSION_ID")
                lastICCSN = p0?.getResultParameter("CardLink::ICCSN")
                lastPersonalData = p0?.getResultParameter("CardLink::PERSONAL_DATA")

                logger.debug { "rn-bridge: personal data: $lastPersonalData" }
                logger.debug { "rn-bridge: wsession_id set to: ${ws_sessionId}" }
                logger.debug { "rn-bridge: lastISSCN set to: ${lastICCSN}" }
                onAuthenticationCompletionCB?.invoke(null, null)
            }
//            onAuthenticationCompletionCB?.invoke(p0?.processResultMinor, p0?.errorMessage)
//            onAuthenticationCompletionCB?.invoke(minor, msg)
        }

        override fun onStarted() {
            logger.debug { "rn-bridge: onStarted" }
            onStartedCB?.invoke()
        }
    }

    var requestCardInsertionCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_requestCardInsertion(cb: Callback) {
        requestCardInsertionCB = cb
    }

    var onCardInteractionCompleteCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onCardInteractionComplete(cb: Callback) {
        onCardInteractionCompleteCB = cb
    }

    var onCardRecognizedCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onCardRecognized(cb: Callback) {
        onCardRecognizedCB = cb
    }

    var onCardInsertedCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onCardInserted(cb: Callback) {
        onCardInsertedCB = cb
    }

    var onCardInsufficientCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onCardInsufficient(cb: Callback) {
        onCardInsufficientCB = cb
    }

    var onCardRemovedCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onCardRemoved(cb: Callback) {
        onCardRemovedCB = cb
    }

    var onCanRequestCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onCanRequest(cb: Callback) {
        onCanRequestCB = cb
    }

    var onCanRetryCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onCanRetry(cb: Callback) {
        onCanRetryCB = cb
    }

    var onPhoneNumberRequestCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onPhoneNumberRequest(cb: Callback) {
        onPhoneNumberRequestCB = cb
    }

    var onPhoneNumberRetryCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onPhoneNumberRetry(cb: Callback) {
        onPhoneNumberRetryCB = cb
    }

    var onSmsCodeRequestCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onSmsCodeRequest(cb: Callback) {
        onSmsCodeRequestCB = cb
    }

    var onSmsCodeRetryCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onSmsCodeRetry(cb: Callback) {
        onSmsCodeRetryCB = cb
    }

    private val cardLinkInteraction = object : CardLinkInteraction {
        override fun requestCardInsertion() {
            logger.debug { "rn-bridge: requestCardInsertion" }
            requestCardInsertionCB?.invoke()
        }

        override fun requestCardInsertion(p0: NFCOverlayMessageHandler?) {
            logger.debug { "rn-bridge: requestCardInsertion" }
            requestCardInsertionCB?.invoke()
        }

        override fun onCardInteractionComplete() {
            logger.debug { "rn-bridge: onCardInteractionComplete" }
            onCardInteractionCompleteCB?.invoke()

        }

        override fun onCardInserted() {
            logger.debug { "rn-bridge: onCardInserted" }
            onCardInsertedCB?.invoke()
        }

        override fun onCardInsufficient() {
            logger.debug { "rn-bridge: onCardInsufficient" }
            onCardInsufficientCB?.invoke()
        }

        override fun onCardRecognized() {
            logger.debug { "rn-bridge: onCardRecognized" }
            onCardRecognizedCB?.invoke()
        }

        override fun onCardRemoved() {
            logger.debug { "rn-bridge: onCardRemoved" }
            onCardRemovedCB?.invoke()
        }

        override fun onCanRequest(p0: ConfirmPasswordOperation?) {
            logger.debug { "rn-bridge: onCanRequest" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "rn-bridge: confirming number $s with framework interaction" }
                    p0.confirmPassword(s)
                }
            }
            onCanRequestCB?.invoke()
        }

        override fun onCanRetry(p0: ConfirmPasswordOperation?, p1: String?, p2: String?) {
            logger.debug { "rn-bridge: onCanRetry" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "rn-bridge: confirming number $s with framework interaction" }
                    p0.confirmPassword(s)
                }
            }
            onCanRetryCB?.invoke(p1, p2)
        }

        override fun onPhoneNumberRequest(p0: ConfirmTextOperation?) {
            logger.debug { "rn-bridge: onPhoneNumberRequest" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "rn-bridge: confirming number $s with framework interaction" }
                    p0.confirmText(s)
                }
            }
            onPhoneNumberRequestCB?.invoke()
        }

        override fun onPhoneNumberRetry(p0: ConfirmTextOperation?, p1: String?, p2: String?) {
            logger.debug { "rn-bridge: onPhoneNumberRetry" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "rn-bridge: confirming number $s with framework interaction" }
                    p0.confirmText(s)
                }
            }
            onPhoneNumberRetryCB?.invoke(p1, p2)
        }

        override fun onSmsCodeRequest(p0: ConfirmPasswordOperation?) {
            logger.debug { "rn-bridge: onSmsCodeRequest" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "rn-bridge: confirming tan $s with framework interaction" }
                    p0.confirmPassword(s)
                }
            }
            onSmsCodeRequestCB?.invoke()
        }

        override fun onSmsCodeRetry(p0: ConfirmPasswordOperation?, p1: String?, p2: String?) {
            logger.debug { "rn-bridge: onSmsCodeRetry" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "rn-bridge: confirming tan $s with framework interaction" }
                    p0.confirmPassword(s)
                }
            }
            onSmsCodeRetryCB?.invoke(p1, p2)
        }

    }
    var onErrorCB: Callback? = null

    @ReactMethod
    fun set_sdkErrorCB(cb: Callback) {
        onErrorCB = cb
    }

    private val errorHandler = object : SdkErrorHandler {
        override fun onError(error: ServiceErrorResponse) {
            logger.debug { "rn-bridge: SdkModule onError will call registered RN callback with: ${error.errorMessage}" }
            onErrorCB?.invoke(error.statusCode.name, error.errorMessage)
        }
    }

    private var userInputDispatch: (s: String) -> Unit = {}

    @ReactMethod
    fun setUserInput(input: String) {
        logger.debug { "rn-bridge: Got user input: $input" }
        userInputDispatch.invoke(input)
    }

    var epothekeInstance: SdkCore? = null

    @ReactMethod
    fun abortCardLink() {
        logger.debug { "rn-bridge: SdkModule abort called $epothekeInstance" }
        epothekeInstance?.cancelOngoingActivation()
    }

    @ReactMethod
    fun activationActive(p: Promise) {
        val res = epothekeInstance?.activationsActive()?.equals(true) ?: false
        p.resolve(res)
    }

    @ReactMethod
    fun destroyCardlinkResources() {
        logger.debug { "rn-bridge: SdkModule desctroy $epothekeInstance" }
        epothekeInstance?.destroyOecContext()
        epothekeInstance = null
    }

    @ReactMethod
    fun startCardLink(cardLinkUrl: String, tenantToken: String) {
        logger.debug { "rn-bridge: SdkModule called with url : $cardLinkUrl" }
        logger.debug { "rn-bridge: SdkModule called with tenantToken: $tenantToken" }

        currentActivity?.let { activity ->
            if (epothekeInstance == null) {
                val epotheke: SdkCore =
                    SdkCore(
                        activity,
                        cardLinkControllerCallback,
                        cardLinkInteraction,
                        errorHandler,
                    )

                epothekeInstance = epotheke

                reactContext.addActivityEventListener(object : ActivityEventListener {
                    override fun onActivityResult(p0: Activity?, p1: Int, p2: Int, p3: Intent?) {
                        //ignore
                    }

                    override fun onNewIntent(p0: Intent?) {
                        p0?.let {
                            epotheke.onNewIntent(p0)
                        }
                    }
                })
                reactContext.addLifecycleEventListener(object : LifecycleEventListener {
                    override fun onHostResume() {
                        epotheke.onResume()
                    }

                    override fun onHostPause() {
                        epotheke.onPause()
                    }

                    override fun onHostDestroy() {
                        epotheke.destroyOecContext()
                    }
                })

            }
            epothekeInstance?.activate(false, cardLinkUrl, tenantToken)
        }
    }

}
