/****************************************************************************
 * Copyright (C) 2024 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the epotheke SDK.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package com.epotheke.sdk

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.mobile.activation.*

private val logger = KotlinLogging.logger {}

class SockError(private val status: ServiceErrorCode, private val errMessage: String?) : ServiceErrorResponse {
    override fun getErrorMessage(): String? {
        return errMessage
    }

    override fun getStatusCode(): ServiceErrorCode {
        return status
    }
}

private class WiredWSListenerImplementation(
    private val ws: WebsocketAndroid,
    private val wsListener: WebsocketListener,
) : WiredWSListener {
    override fun onOpen() = wsListener.onOpen(ws)
    override fun onClose(code: Int, reason: String?) = wsListener.onClose(ws, code, reason)
    override fun onError(error: String) = wsListener.onError(ws, error)
    override fun onText(msg: String) = wsListener.onText(ws, msg)
}

class WebsocketListenerAndroid(private val wsListenerCommon: WebsocketListenerCommon) : WebsocketListener {

    override fun onOpen(p0: Websocket) = wsListenerCommon.onOpen()
    override fun onClose(p0: Websocket, p1: Int, p2: String?) = wsListenerCommon.onClose(p1, p2)
    override fun onError(p0: Websocket, p1: String) = wsListenerCommon.onError(p1)
    override fun onText(p0: Websocket, p1: String) = wsListenerCommon.onText(p1)

}

class WebsocketAndroid(
    private val commonWS: WebsocketCommon,
    private val sdkErrorHandler: SdkErrorHandler,
): Websocket {

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
        } catch (e: Exception) {
//            throw WebsocketException(e.message ?: "Unknown error", e)
            sdkErrorHandler.onError(SockError(
                if (e.message?.contains("401") == true) ServiceErrorCode.NOT_AUTHORIZED else  ServiceErrorCode.NO_CONNECTION,
                e.message
            ))
        }
    }

    @Throws(WebsocketException::class)
    override fun close(statusCode: Int, reason: String?) {
        try {
            commonWS.close(statusCode, reason)
        } catch (e: Exception) {
            //throw WebsocketException(e.message ?: "Unknown error", e)
            sdkErrorHandler.onError(SockError(ServiceErrorCode.LOST_CONNECTION, e.message))
        }
    }

    @Throws(WebsocketException::class)
    override fun send(data: String) {
        logger.debug { "sending via socket: $data" }
        try {
            commonWS.send(data)
        } catch (e: Exception) {
            //throw WebsocketException(e.message ?: "Unknown error", e)
            sdkErrorHandler.onError(SockError(ServiceErrorCode.LOST_CONNECTION, e.message))
        }
    }
}

