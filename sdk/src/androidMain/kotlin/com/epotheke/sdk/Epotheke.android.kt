package com.epotheke.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Parcelable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.android.activation.AndroidContextManager
import org.openecard.android.activation.OpeneCard
import org.openecard.android.utils.NfcIntentHelper
import org.openecard.mobile.activation.*


private val logger = KotlinLogging.logger {}
class WebsocketListenerImp: WebsocketListener{
    override fun onOpen(p0: Websocket?) {
        println("open")
    }

    override fun onClose(p0: Websocket?, p1: Int, p2: String?) {
    }

    override fun onError(p0: Websocket?, p1: String?) {
    }

    override fun onText(p0: Websocket?, p1: String?) {
        println(p1)
        p0?.send("answer")
    }
}

abstract class EpothekeActivity : Activity() {
    var oec: OpeneCard? = null
    var ctxManager: AndroidContextManager? = null
    var activationSource: ActivationSource? = null
    var nfcIntentHelper : NfcIntentHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initOecContext()
    }

    override fun onDestroy() {
        ctxManager?.terminateContext(object : StopServiceHandler {
            override fun onSuccess() {
                // do nothing
                logger.debug { "Open eCard stopped successfully." }
                cleanupOecInstances()
            }
            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to stop Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
            }
        })
        super.onDestroy()
    }

    protected fun initOecContext() {
        oec = OpeneCard.createInstance()
        nfcIntentHelper = NfcIntentHelper.create(this)
        ctxManager = oec?.context(this)
        ctxManager?.initializeContext(object : StartServiceHandler {
            override fun onSuccess(actSource: ActivationSource) {
                activationSource = actSource
                val websocket = WebsocketAndroid(getCardlinkUrl())
                actSource.cardLinkFactory().create(websocket, getControllerCallback(), overridingCardlinIteraction(), WebsocketListenerImp())
            }
            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to initialize Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
            }
        })
    }

    protected fun cleanupOecInstances() {
        oec = null
        ctxManager = null
        activationSource = null
    }

    @SuppressLint("NewApi")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val t : Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        t?.let {
            ctxManager?.onNewIntent(intent)
        }
    }

    //maybe use delegate here see: https://kotlinlang.org/docs/delegation.html#overriding-a-member-of-an-interface-implemented-by-delegation
    private fun overridingCardlinIteraction(): CardLinkInteraction {
        val appImplementation = getCardLinkInteraction()
        return object: CardLinkInteraction{
            override fun requestCardInsertion() {
                nfcIntentHelper?.enableNFCDispatch()
                appImplementation.requestCardInsertion()
            }
            override fun requestCardInsertion(p0: NFCOverlayMessageHandler?) = appImplementation.requestCardInsertion(p0)
            override fun onCardInteractionComplete() {
                nfcIntentHelper?.disableNFCDispatch()
                appImplementation.onCardInteractionComplete()
            }
            override fun onCardRecognized() = appImplementation.onCardRecognized()
            override fun onCardRemoved() = appImplementation.onCardRemoved()
            override fun onCanRequest(p0: ConfirmPasswordOperation?) = appImplementation.onCanRequest(p0)
            override fun onPhoneNumberRequest(p0: ConfirmTextOperation?) = appImplementation.onPhoneNumberRequest(p0)
            override fun onSmsCodeRequest(p0: ConfirmPasswordOperation?) = appImplementation.onSmsCodeRequest(p0)
        }
    }

    abstract fun getCardlinkUrl(): String;
    abstract fun getControllerCallback() : ControllerCallback;
    abstract fun getCardLinkInteraction() : CardLinkInteraction;
    abstract fun getProtocols(): Set<CardLinkProtocol>

}
