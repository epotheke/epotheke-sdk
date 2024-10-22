package com.demoreactnative

import android.app.Activity
import android.content.Intent
import com.epotheke.erezept.model.AvailablePrescriptionLists
import com.epotheke.erezept.model.RequestPrescriptionList
import com.epotheke.erezept.model.SelectedPrescriptionList
import com.epotheke.erezept.model.prescriptionJsonFormatter
import com.epotheke.sdk.CardLinkProtocol
import com.epotheke.sdk.CardLinkControllerCallback
import com.epotheke.sdk.SdkCore
import com.epotheke.sdk.PrescriptionProtocol
import com.epotheke.sdk.SdkErrorHandler
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
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

    @ReactMethod
    fun getPrescriptions(p: Promise) {
        runBlocking {
            callPrescriptionProtocolNullChecked(p) {
                try {
                    val availablePrescriptions: AvailablePrescriptionLists =
                        requestPrescriptions(RequestPrescriptionList())
                    p.resolve(
                        prescriptionJsonFormatter.encodeToString(availablePrescriptions)
                    )
                } catch (e: Exception){
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
                    val confirmation = selectPrescriptions(prescriptionJsonFormatter.decodeFromString<SelectedPrescriptionList>(selection))
                    p.resolve(
                        prescriptionJsonFormatter.encodeToString(confirmation)
                    )
                } catch (e: Exception){
                    p.reject(e)
                }
            }
        }
    }

    private suspend fun callPrescriptionProtocolNullChecked(p: Promise, call: suspend PrescriptionProtocol.() -> Unit){
        when (val proto = erezeptProtocol) {
            null -> {
                p.reject("Protocol not available, is CardLink established?")
            }
            else -> proto.call()
        }
    }

    private val cardLinkControllerCallback = object : CardLinkControllerCallback {
        override fun onAuthenticationCompletion(
            p0: ActivationResult?,
            cardLinkProtocols: Set<CardLinkProtocol>
        ) {
            logger.debug { "onAuthenticationCompletion ${p0?.errorMessage}" }

            erezeptProtocol = cardLinkProtocols.filterIsInstance<PrescriptionProtocol>().first()

            val availableProtocols =
                cardLinkProtocols.joinToString(prefix = "protocols: ") { p -> p.javaClass.name }
            onAuthenticationCompletionCB?.invoke(p0?.errorMessage, availableProtocols)

        }

        override fun onStarted() {
            logger.debug { "onStarted" }
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

    var onPhoneNumberRequestCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onPhoneNumberRequest(cb: Callback) {
        onPhoneNumberRequestCB = cb
    }

    var onSmsCodeRequestCB: Callback? = null

    @ReactMethod
    fun set_cardlinkInteractionCB_onSmsCodeRequest(cb: Callback) {
        onSmsCodeRequestCB = cb
    }

    private val cardLinkInteraction = object : CardLinkInteraction {
        override fun requestCardInsertion() {
            logger.debug { "requestCardInsertion" }
            requestCardInsertionCB?.invoke()
        }

        override fun requestCardInsertion(p0: NFCOverlayMessageHandler?) {
            logger.debug { "requestCardInsertion" }
            requestCardInsertionCB?.invoke()
        }

        override fun onCardInteractionComplete() {
            logger.debug { "onCardInteractionComplete" }
            onCardInteractionCompleteCB?.invoke()

        }

        override fun onCardRecognized() {
            logger.debug { "onCardRecognized" }
            onCardRecognizedCB?.invoke()
        }

        override fun onCardRemoved() {
            logger.debug { "onCardRemoved" }
            onCardRemovedCB?.invoke()
        }

        override fun onCanRequest(p0: ConfirmPasswordOperation?) {
            logger.debug { "onCanRequest" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "confirming number $s with framework interaction" }
                    p0.confirmPassword(s)
                }
            }
            onCanRequestCB?.invoke()
        }

        override fun onPhoneNumberRequest(p0: ConfirmTextOperation?) {
            logger.debug { "onPhoneNumberRequest" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "confirming number $s with framework interaction" }
                    p0.confirmText(s)
                }
            }
            onPhoneNumberRequestCB?.invoke()
        }

        override fun onSmsCodeRequest(p0: ConfirmPasswordOperation?) {
            logger.debug { "onSmsCodeRequest" }
            p0?.let {
                userInputDispatch = { s ->
                    logger.debug { "confirming tan $s with framework interaction" }
                    p0.confirmPassword(s)
                }
            }
            onSmsCodeRequestCB?.invoke()
        }

    }
    var onErrorCB: Callback? = null

    @ReactMethod
    fun set_sdkErrorCB(cb: Callback) {
        onErrorCB = cb
    }

    private val errorHandler = object : SdkErrorHandler {
        override fun onError(error: ServiceErrorResponse) {
            logger.debug { "SdkModule onError will call registered RN callback with: ${error.errorMessage}" }
            onErrorCB?.invoke(null, error.errorMessage)
        }
    }

    private var userInputDispatch: (s: String) -> Unit = {}

    @ReactMethod
    fun setUserInput(input: String) {
        logger.debug { "Got user input: $input" }
        userInputDispatch.invoke(input)
    }

    var epothekeInstance : SdkCore? = null
    @ReactMethod
    fun startCardLink(cardLinkUrl: String, tenantToken: String?) {
        logger.debug { "SdkModule called with url : $cardLinkUrl" }
        logger.debug { "SdkModule called with tenantToken: $tenantToken" }

        epothekeInstance?.let {
            it.destroyOecContext()
        }

        currentActivity?.let { activity ->
            val epotheke = SdkCore(
                activity,
                cardLinkUrl,
                tenantToken,
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

            epotheke.initOecContext()
        }
    }

}
