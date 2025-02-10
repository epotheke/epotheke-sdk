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

import cocoapods.open_ecard.ServiceErrorCode
import cocoapods.open_ecard.ServiceErrorResponseProtocol
import cocoapods.open_ecard.WebsocketProtocol
import cocoapods.open_ecard.WebsocketListenerProtocol
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.websocket.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.NSObject

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalForeignApi::class)
class SockError(private val code: ServiceErrorCode, private val errMessage: String?) : ServiceErrorResponseProtocol, NSObject() {
    override fun getErrorMessage(): String? {
        return errMessage
    }

    override fun getStatusCode(): ServiceErrorCode {
        return code
    }
}

@OptIn(ExperimentalForeignApi::class)
private class WiredWSListenerImplementation constructor(
    private val ws: WebsocketIos,
    private val wsListener: WebsocketListenerProtocol,
) : WiredWSListener {
    override fun onOpen() = wsListener.onOpen(ws)
    override fun onClose(code: Int, reason: String?) = wsListener.onClose(ws, code, reason)
    override fun onError(error: String) = wsListener.onError(ws, error)
    override fun onText(msg: String) = wsListener.onText(ws, msg)
}

@OptIn(ExperimentalForeignApi::class)
class WebsocketListenerIos(private val wsListenerCommon: WebsocketListenerCommon) : WebsocketListenerProtocol,
    NSObject() {
    override fun onOpen(webSocket: NSObject?) = wsListenerCommon.onOpen()
    override fun onClose(webSocket: NSObject?, withStatusCode: Int, withReason: String?) =
        wsListenerCommon.onClose(withStatusCode, withReason)

    override fun onError(webSocket: NSObject?, withError: String?) =
        wsListenerCommon.onError(withError ?: "unknown error")

    override fun onText(webSocket: NSObject?, withData: String?) =
        wsListenerCommon.onText(withData ?: "invalid message")
}

@OptIn(ExperimentalForeignApi::class)
class WebsocketIos(
    private val commonWS: WebsocketCommon,
    private val errorHandler: SdkErrorHandler,
) : NSObject(), WebsocketProtocol {


    private fun setListener(wsListener: WebsocketListenerProtocol) =
        commonWS.setListener(WiredWSListenerImplementation(this, wsListener))

    override fun removeListener() = commonWS.removeListener()
    override fun getUrl(): String = commonWS.getUrl()
    override fun setUrl(url: String?) = commonWS.setUrl(url ?: "")
    override fun getSubProtocol(): String? = commonWS.getSubProtocol()
    override fun isOpen(): Boolean = commonWS.isOpen()
    override fun isFailed(): Boolean = commonWS.isFailed()

    //    @Throws(WebsocketException::class)
    override fun connect() {
        try {
            commonWS.connect()
        } catch (e: Exception) {
            logger.warn(e) {"Websocket connection failed."}
            val code = if(e.message?.contains("-1011") == true)
                ServiceErrorCode.kServiceErrorCodeNOT_AUTHORIZED.name.replace("kServiceErrorCode","")
                else
                ServiceErrorCode.kServiceErrorCodeNO_CONNECTION.name.replace("kServiceErrorCode","")
            errorHandler.hdl(
                code,
                e.message ?: "no message"
            )
      }
    }

    //    @Throws(WebsocketException::class)
    override fun close(statusCode: Int, withReason: String?) {
        try {
            commonWS.close(statusCode, withReason)
        } catch (e: Exception) {
            logger.warn(e) {"Websocket close failed."}
            errorHandler.hdl(
                ServiceErrorCode.kServiceErrorCodeLOST_CONNECTION.name.replace("kServiceErrorCode", ""),
                e.message?: "no message"
            )
        }
    }

    //   @Throws(WebsocketException::class)
    override fun send(data: String?) {
        try {
            data?.let {
                commonWS.send(data)
            }
        } catch (e: Exception) {
            logger.warn(e) {"Websocket send failed."}
            errorHandler.hdl(
                ServiceErrorCode.kServiceErrorCodeLOST_CONNECTION.name.replace("kServiceErrorCode",""),
                e.message ?: "no message"
            )
        }
    }

    override fun isClosed(): Boolean {
        return !commonWS.isOpen()
    }

    override fun setListener(listener: NSObject?) {
        listener?.let {
            this.setListener(listener as WebsocketListenerProtocol)
        }
    }
}
