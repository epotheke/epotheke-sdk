package com.epotheke.sdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.android.activation.AndroidContextManager
import org.openecard.android.activation.OpeneCard
import org.openecard.mobile.activation.*
import org.openecard.mobile.activation.WebsocketListener


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
        ctxManager = oec?.context(this)
        ctxManager?.initializeContext(object : StartServiceHandler {
            override fun onSuccess(actSource: ActivationSource) {
                activationSource = actSource
                val websocket = WebsocketAndroid(getCardlinkUrl())
                actSource.cardLinkFactory().create(websocket, getControllerCallback(), getCardLinkInteraction(), WebsocketListenerImp())
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ctxManager?.onNewIntent(intent)
    }



    abstract fun getCardlinkUrl(): String;
    abstract fun getControllerCallback() : ControllerCallback;
    abstract fun getCardLinkInteraction() : CardLinkInteraction;
    abstract fun getProtocols(): Set<CardLinkProtocol>

//    fun cardlinkConnect(wsUrl: String,  ctrlCb: ControllerCallback, interaction: CardLinkInteraction, protocols: Set<CardLinkProtocol>){
//        //match protocol to type and build use wslistener to execute it
//
//        when (protocols.first()) {
//            is ErezeptProtocol -> {}
//        }
//
//        val ws = WebsocketAndroid(wsUrl);
////        ws.setListener(wsListener);
//        this.activationSource?.let {
//            it.cardLinkFactory().create(ws, ctrlCb, interaction)
//        }
//    }
}
