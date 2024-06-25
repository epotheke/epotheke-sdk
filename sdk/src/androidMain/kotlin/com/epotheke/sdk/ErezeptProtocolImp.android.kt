package com.epotheke.sdk

import com.epotheke.erezept.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}
private val ReceiveTimeout = Duration.parse("30s")

class ErezeptProtocolImp(
    private val ws: WebsocketAndroid,
) : ErezeptProtocol {

    private val inputChannel = Channel<String>()
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun requestReceipts(req: RequestPrescriptionList): AvailablePrescriptionLists {
        logger.debug { "Sending data to request ecreipts." }

        ws.send(
            eRezeptJsonFormatter.encodeToString(req)
        )

        while (true) {
            try {
                val response = withTimeout(
                    timeout = ReceiveTimeout
                ){
                    inputChannel.receive()
                }
                when (val eRezeptMessage = eRezeptJsonFormatter.decodeFromString<ERezeptMessage>(response)) {
                    is AvailablePrescriptionLists -> {
                        if (eRezeptMessage.correlationId == req.messageId) {
                            return eRezeptMessage
                        }
                    }
                    is GenericErrorMessage -> {
                        throw ErezeptProtocolException(eRezeptMessage)
                    }
                    else -> continue
                }
            } catch (e: Exception) {
                when (e) {
                    is TimeoutException -> {
                        logger.error { "Timeout during receive" }
                        throw ErezeptProtocolException(GenericErrorMessage(
                            errorCode = GenericErrorResultType.UNKNOWN_ERROR,
                            errorMessage = "Timeout",
                            correlationId = req.messageId
                        ))
                    }
                }
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
                val response = withTimeout(
                    timeout = ReceiveTimeout
                ){
                    inputChannel.receive()
                }
                when (val eRezeptMessage = eRezeptJsonFormatter.decodeFromString<ERezeptMessage>(response)) {
                    is ConfirmPrescriptionList -> {
                        if (eRezeptMessage.correlationId == lst.messageId) {
                            return eRezeptMessage
                        }
                    }
                    is GenericErrorMessage -> {
                        throw ErezeptProtocolException(eRezeptMessage)
                    }
                    else -> continue
                }
            } catch (e: Exception) {
                when (e) {
                    is TimeoutException -> {
                        logger.error { "Timeout during receive" }
                        throw ErezeptProtocolException(GenericErrorMessage(
                            errorCode = GenericErrorResultType.UNKNOWN_ERROR,
                            errorMessage = "Timeout",
                            correlationId = lst.messageId
                        ))
                    }
                }
                logger.error(e) { "Exception during receive" }
                throw e
            }
        }
    }

    override fun registerListener(channelDispatcher: ChannelDispatcher) {
        channelDispatcher.addProtocolChannel(inputChannel)
    }
}

