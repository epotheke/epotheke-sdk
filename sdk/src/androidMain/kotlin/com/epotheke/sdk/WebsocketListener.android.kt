package com.epotheke.sdk

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.openecard.mobile.activation.Websocket

private val logger = KotlinLogging.logger {}

class WebsocketListener() : ChannelDispatcher,
    org.openecard.mobile.activation.WebsocketListener {

    private val channels: MutableList<Channel<String>> = ArrayList<Channel<String>>()

    override fun onOpen(p0: Websocket) {
    }

    override fun onClose(p0: Websocket, p1: Int, p2: String?) {
    }

    override fun onError(p0: Websocket, p1: String) {
//        protos.map { p-> p.getErrorHandler()(p1) }
    }

    override fun onText(p0: Websocket, p1: String) {
        logger.debug { "Message from established link: $p1" }
        runBlocking {
            channels.map { c ->
                c.send(p1)
            }
        }
    }

    override fun addProtocolChannel(channel: Channel<String>) {
        channels.add(channel)
    }
}
