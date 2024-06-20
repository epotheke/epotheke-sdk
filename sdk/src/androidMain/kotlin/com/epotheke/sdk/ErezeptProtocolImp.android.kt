package com.epotheke.sdk

import ErezeptProtocol
import com.epotheke.erezept.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel

private val logger = KotlinLogging.logger {}

class ErezeptProtocolImp(
    private val ws: WebsocketAndroid,
) : ErezeptProtocol {

    private var inputChannel: Channel<String>? = null

    override suspend fun requestReceipts(req: RequestPrescriptionList) : AvailablePrescriptionList? {
        logger.debug { "Sending data to request ecreipts." }

        if (inputChannel == null){
            inputChannel = Channel<String>()
        }

        ws.send(RequestPrescriptionList(
            "type" ,
            messageId = "messageId"
        ).toString())

        val response = inputChannel?.receive()
        //parse rsponse and look for correct answer

        //return response
        return null

    }

    override suspend fun selectReceipts(lst: SelectedPrescriptionList) {
        logger.debug { "Sending data to select ecreipts." }
        ws.send(lst.toString())
    }

    override fun getInputChannel(): Channel<String>? {
        return inputChannel
    }

}

