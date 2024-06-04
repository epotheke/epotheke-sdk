package com.epotheke.sdk

import WebsocketCommon
import WiredWSListener
import kotlinx.coroutines.*
import org.openecard.mobile.activation.Websocket
import org.openecard.mobile.activation.WebsocketException
import org.openecard.mobile.activation.WebsocketListener

private class WiredWSListenerImplementation(
    private val ws: WebsocketAndroid,
    private val wsListener: WebsocketListener,
) : WiredWSListener {
    override fun onOpen() = wsListener.onOpen(ws)
    override fun onClose(code: Int, reason: String?) = wsListener.onClose(ws, code, reason)
    override fun onError(error: String?) = wsListener.onError(ws, error)
    override fun onText(msg: String?) = wsListener.onText(ws, msg)
}


class WebsocketAndroid(
    url: String,
) : Websocket {

    private val commonWS = WebsocketCommon(url)

    override fun setListener(wsListener: WebsocketListener) =
        commonWS.setListener(WiredWSListenerImplementation(this, wsListener))
    override fun removeListener() = commonWS.removeListener()
    override fun getUrl(): String = commonWS.getUrl()
    override fun setUrl(url: String) = commonWS.setUrl(url)

    override fun getSubProtocol(): String? = commonWS.getSubProtocol()
    override fun isOpen(): Boolean = commonWS.isOpen()
    override fun isFailed(): Boolean = commonWS.isFailed()

    @Throws(WebsocketException::class)
    override fun connect() {
        try {
            commonWS.connect()
        } catch (e : Exception){
            throw WebsocketException(e.message)
        }
    }
    @Throws(WebsocketException::class)
    override fun close(statusCode: Int, reason: String?) {
        try {
            commonWS.close(statusCode, reason)
        } catch (e : Exception){
            throw WebsocketException(e.message)
        }
    }

    @Throws(WebsocketException::class)
    override fun send(data: String) {
        try{
            commonWS.send(data)
        } catch (e : Exception){
            throw WebsocketException(e.message)
        }
    }
}

