package com.epotheke.sdk

import com.epotheke.erezept.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString

private val logger = KotlinLogging.logger {}

class ErezeptProtocolImp(
    private val ws: WebsocketAndroid,
) : ErezeptProtocol {

    private val inputChannel = Channel<String>()

    override suspend fun requestReceipts(req: RequestPrescriptionList): AvailablePrescriptionLists {
        logger.debug { "Sending data to request ecreipts." }

        ws.send(
            eRezeptJsonFormatter.encodeToString(req)
        )

        while (true) {
            try {
                val response = inputChannel.receive()
                when (val eRezeptMessage = eRezeptJsonFormatter.decodeFromString<ERezeptMessage>(response)) {
                    is AvailablePrescriptionLists -> {
                        if (eRezeptMessage.correlationId == req.messageId) {
                            return eRezeptMessage
                        }
                    }

                    is GenericErrorMessage -> {
                        throw ErezeptProtocolException(eRezeptMessage)
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Exceptino during receive" }
                throw e
            }
        }
    }

    override suspend fun selectReceipts(lst: SelectedPrescriptionList): ConfirmPrescriptionList {
        logger.debug { "Sending data to select ecreipts." }
        ws.send(
            eRezeptJsonFormatter.encodeToString(lst)
        )
        while (true) {
            try {
                val response = inputChannel.receive()
                when (val eRezeptMessage = eRezeptJsonFormatter.decodeFromString<ERezeptMessage>(response)) {
                    is ConfirmPrescriptionList -> {
                        if (eRezeptMessage.correlationId == lst.messageId) {
                            return eRezeptMessage
                        }
                    }

                    is GenericErrorMessage -> {
                        throw ErezeptProtocolException(eRezeptMessage)
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Exception during receive" }
                throw e
            }
        }
    }

    override fun registerListener(channelDispatcher: ChannelDispatcher) {
        channelDispatcher.addProtocolChannel(inputChannel)
    }
}

