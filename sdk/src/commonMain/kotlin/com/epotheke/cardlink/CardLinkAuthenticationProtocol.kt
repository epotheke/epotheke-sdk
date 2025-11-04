package com.epotheke.cardlink

import com.epotheke.CardLinkProtocolBase
import com.epotheke.Websocket
import com.epotheke.cardlink.CardLinkAuthenticationConfig.readInsurerData
import com.epotheke.cardlink.CardLinkAuthenticationConfig.readPersonalData
import com.epotheke.prescription.prescriptionJsonFormatter
import com.epotheke.protocolChannel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.openecard.sal.iface.DeviceUnavailable
import org.openecard.sal.iface.DeviceUnsupported
import org.openecard.sal.sc.SmartcardSalSession
import org.openecard.sc.iface.ReaderUnavailable
import org.openecard.sc.iface.SmartCardStackMissing
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val MESSAGE_TIMEOUT_DURATION = 5.seconds

private val logger = KotlinLogging.logger { }

class CardLinkAuthenticationProtocol internal constructor(
    private val ws: Websocket,
    private val salSession: SmartcardSalSession,
) : CardLinkProtocolBase(ws) {
    data class SessionInfo(
        val cardSessionId: String,
        val webSocketId: String? = null,
        val phoneRegistered: Boolean = false,
    )

    private val inputChannel = protocolChannel<GematikEnvelope>()

    override fun messageHandler(msg: String): (suspend () -> Unit)? {
        try {
            val envelope = prescriptionJsonFormatter.decodeFromString<GematikEnvelope>(msg)
            return { inputChannel.send(envelope) }
        } catch (_: Exception) {
            return null
        }
    }

    private lateinit var sessionInfo: SessionInfo
    private val cardLinkAuthResult = CardLinkAuthResult()
    private lateinit var interaction: UserInteraction

    @Throws(
        CardLinkError::class,
        CardLinkClientError::class,
        CancellationException::class,
    )
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun establishCardLink(interaction: UserInteraction): CardLinkAuthResult =
        mapErrors {
            this.interaction = interaction
            // note: We do a reconnect here. Sine the service informs us about whether phone is registered or not only
            // on a fresh connect with the sessionInfo message. Since we however store the wsSession-id we use the same
            // session as long as it is not overridden by the server or Epotheke instance was recreated with another one
            ws.connectWithTimeout()

            sessionInfo =
                receiveSessionInformation().apply {
                    cardLinkAuthResult.wsSessionId = webSocketId
                }

            if (!sessionInfo.phoneRegistered) {
                requestTan()
                confirmTan()
            }

            withAuthenticatedEgk(
                salSession,
                interaction,
            ) {
                if (readPersonalData) {
                    cardLinkAuthResult.personalData = personalData
                }
                if (readInsurerData) {
                    cardLinkAuthResult.insurerData = insurerData
                }

                cardLinkAuthResult.iccsn = iccsn

                RegisterEgk(
                    sessionInfo.cardSessionId,
                    gdo = mfData.gdo.toByteArray(),
                    cardVersion = mfData.cardVersion.toByteArray(),
                    cvcAuth = mfData.cvcAuth.toByteArray(),
                    cvcCA = mfData.cvcCA.toByteArray(),
                    atr = mfData.atr.toByteArray(),
                    x509AuthECC = cert.toByteArray(),
                    x509AuthRSA = null,
                ).send()

                handleRemoteApdus()
            }

            cardLinkAuthResult
        }

    private inline fun mapErrors(block: () -> CardLinkAuthResult): CardLinkAuthResult {
        try {
            return block()
        } catch (e: Exception) {
            logger.error(e) { "Mapping error" }
            throw when (e) {
                is CancellationException,
                is CardLinkError,
                is CardLinkClientError,
                -> throw e

                is TimeoutCancellationException -> Timeout(cause = e)
                // wrong card
                is DeviceUnsupported -> CardInsufficient(cause = e)
                // appears if card gets lost
                is DeviceUnavailable -> CardRemoved(cause = e)
                is ReaderUnavailable -> OtherNfcError(cause = e, message = "NFC not available or turned off.")
                is SmartCardStackMissing -> OtherNfcError(cause = e)

                else -> OtherClientError(cause = e)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun CardLinkPayload.send(correlationId: String? = Uuid.random().toString()) {
        GematikEnvelope(
            this,
            correlationId,
            sessionInfo.cardSessionId,
        ).send()
    }

    private suspend fun GematikEnvelope.send() {
        ws.send(
            cardLinkJsonFormatter.encodeToString(this),
        )
    }

    private suspend fun receiveEnvelope(
        correlationId: String? = null,
        ignoreSessionInfo: Boolean = true,
    ): GematikEnvelope =
        withTimeout(MESSAGE_TIMEOUT_DURATION) {
            val envelope = inputChannel.receive()

            if (ignoreSessionInfo && envelope.payload is SessionInformation) {
                receiveEnvelope(correlationId, true)
            } else {
                if (correlationId != null) {
                    if (correlationId == envelope.correlationId) {
                        envelope
                    } else {
                        throw CorrelationIdMismatch()
                    }
                } else {
                    envelope
                }
            }
        }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun receiveSessionInformation(): SessionInfo {
        val egkEnvelope =
            try {
                receiveEnvelope(ignoreSessionInfo = false)
                // If server doesn't send SessionInfo we create our own
            } catch (e: TimeoutCancellationException) {
                null
            }

        return when (val payload = egkEnvelope?.payload) {
            is SessionInformation -> {
                logger.debug {
                    "Using ${egkEnvelope.cardSessionId ?: "random cardSessionID as it was null"} as cardSessionId and ${payload.webSocketId} as webSocketId."
                }
                SessionInfo(
                    egkEnvelope.cardSessionId ?: Uuid.random().toString(),
                    payload.webSocketId,
                    payload.phoneRegistered,
                )
            }

            else -> {
                val cardSessionId = Uuid.random().toString()
                logger.warn {
                    "Received no or a malformed SessionInformation message. Using $cardSessionId as cardSessionId."
                }
                SessionInfo(cardSessionId)
            }
        }
    }

    private fun handleTaskListError(egkPayload: TasklistErrorPayload): Nothing =
        throw CardLinkError.byCode(egkPayload.status, egkPayload.errormessage)

    private suspend fun requestTan(
        lastResultCode: ResultCode? = null,
        lastErrorMessage: String? = null,
    ) {
        val number =
            when (lastResultCode) {
                null ->
                    interaction.onPhoneNumberRequest()

                else ->
                    interaction.onPhoneNumberRetry(lastResultCode, lastErrorMessage ?: "")
            }
        SendPhoneNumber(
            number,
        ).send()

        val egkEnvelopeResponse = receiveEnvelope()

        when (val egkPayload = egkEnvelopeResponse.payload) {
            is TasklistErrorPayload -> handleTaskListError(egkPayload)
            is ConfirmPhoneNumber -> {
                when (egkPayload.resultCode) {
                    ResultCode.SUCCESS -> { // nothing we are happy
                    }

                    ResultCode.NUMBER_BLOCKED,
                    ResultCode.INVALID_REQUEST,
                    ResultCode.NUMBER_FROM_WRONG_COUNTRY,
                    -> {
                        // retry
                        requestTan(egkPayload.resultCode, egkPayload.errorMessage)
                    }

                    else -> handleInvalidResultCode(egkPayload.resultCode)
                }
            }

            else -> handleInvalidMessage(egkPayload)
        }
    }

    private fun handleInvalidMessage(egkPayload: CardLinkPayload?) {
        val errMsg = "Received invalid message from service"
        logger.error { errMsg }
        logger.debug {
            "Message was $egkPayload which is no valid message at this step."
        }
        throw InvalidWebsocketMessage()
    }

    private fun handleInvalidResultCode(code: ResultCode?) {
        val errMsg = "Received invalid result code from service"
        logger.error { errMsg }
        logger.debug {
            "Result code was $code which is no valid answer at this step."
        }
        throw InvalidWebsocketMessage()
    }

    private suspend fun confirmTan(
        lastResultCode: ResultCode? = null,
        lastErrorMessage: String? = null,
    ) {
        val userEnteredTan =
            when (lastResultCode) {
                null -> interaction.onTanRequest()
                else -> {
                    interaction.onTanRetry(
                        lastResultCode,
                        lastErrorMessage,
                    )
                }
            }

        SendTan(
            smsCode = userEnteredTan,
            tan = userEnteredTan,
        ).send()

        val response = receiveEnvelope()
        when (val egkPayload = response.payload) {
            is TasklistErrorPayload -> handleTaskListError(egkPayload)
            is ConfirmTan -> {
                when (val resultCode = egkPayload.resultCode) {
                    ResultCode.SUCCESS -> { // do nothing
                    }
                    ResultCode.INVALID_REQUEST,
                    ResultCode.TAN_INCORRECT,
                    -> {
                        // retry
                        confirmTan(egkPayload.resultCode, egkPayload.errorMessage)
                    }
                    ResultCode.TAN_EXPIRED -> throw TanExpired()
                    ResultCode.TAN_RETRY_LIMIT_EXCEEDED -> throw TanRetryLimitExceeded()

                    else -> handleInvalidResultCode(resultCode)
                }
            }

            else -> handleInvalidMessage(egkPayload)
        }
    }

    private suspend fun Egk.handleRemoteApdus() {
        val envelope = receiveEnvelope()
        when (val payload = envelope.payload) {
            is ICCSNReassignment -> {
                logger.debug { "Received '${ICCSN_REASSIGNMENT}' message from CardLink service." }
                cardLinkAuthResult.iccsnReassignmentTimestamp = payload.lastAssignment
                handleRemoteApdus()
            }
            is RegisterEgkFinish -> {
                // ready
                logger.debug { "Received '${REGISTER_EGK_FINISH}' message from CardLink service." }
                return
            }
            is SendApdu -> {
                val cardResponse = sendApduToCard(payload.apdu)
                SendApduResponse(
                    payload.cardSessionId,
                    cardResponse,
                ).send(envelope.correlationId)

                handleRemoteApdus()
            }

            is TasklistErrorPayload -> {
                logger.error { "Received task list error: ${payload.errormessage}" }
                handleTaskListError(payload)
            }
            else -> {
                logger.warn { "Received unexpected message: $payload - will continue." }
                handleRemoteApdus()
            }
        }
    }
}
