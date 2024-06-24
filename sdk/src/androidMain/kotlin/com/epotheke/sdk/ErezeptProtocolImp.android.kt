package com.epotheke.sdk

import ChannelDispatcher
import ErezeptProtocol
import ErezeptProtocolException
import com.epotheke.erezept.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel

private val logger = KotlinLogging.logger {}

class ErezeptProtocolImp(
    private val ws: WebsocketAndroid,
) : ErezeptProtocol {

    private val inputChannel = Channel<String>()

    override suspend fun requestReceipts(req: RequestPrescriptionList) : AvailablePrescriptionLists {
        logger.debug { "Sending data to request ecreipts." }
        ws.send(req.toString())

        while (true) {
            try{
                val response = inputChannel.receive()
                val eRezeptMessage = eRezeptJsonFormatter.decodeFromString<ERezeptMessage>(response)

                when (eRezeptMessage) {
                    is AvailablePrescriptionLists -> {
                        if(eRezeptMessage.correlationId == req.messageId){
                            return eRezeptMessage
                        }
                    }
                    is GenericErrorMessage -> {
                        ErezeptProtocolException(eRezeptMessage)
                    }
                }
            }catch (e: Exception){
                logger.error(e) { "Exceptino during receive"}
            }
        }
    }

    override suspend fun selectReceipts(lst: SelectedPrescriptionList) {
        logger.debug { "Sending data to select ecreipts." }
        ws.send(lst.toString())
    }

    override fun registerListener(channelDispatcher: ChannelDispatcher) {
        channelDispatcher.addProtocolChannel(inputChannel)
    }

}

