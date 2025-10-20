package com.epotheke.cardlink

import com.epotheke.CardLinkProtocolBase
import com.epotheke.Websocket
import com.epotheke.prescription.model.prescriptionJsonFormatter
import com.epotheke.protocolChannel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.GzipSource
import org.openecard.cif.bundled.CompleteTree
import org.openecard.cif.bundled.EgkCif
import org.openecard.cif.bundled.EgkCifDefinitions
import org.openecard.cif.bundled.EgkCifDefinitions.cardType
import org.openecard.cif.definition.acl.DidStateReference
import org.openecard.cif.definition.recognition.removeUnsupported
import org.openecard.sal.iface.DeviceUnavailable
import org.openecard.sal.iface.DeviceUnsupported
import org.openecard.sal.iface.MissingAuthentications
import org.openecard.sal.iface.dids.PaceDid
import org.openecard.sal.sc.SmartcardApplication
import org.openecard.sal.sc.SmartcardDataset
import org.openecard.sal.sc.SmartcardDeviceConnection
import org.openecard.sal.sc.SmartcardSal
import org.openecard.sal.sc.recognition.DirectCardRecognition
import org.openecard.sc.apdu.toCommandApdu
import org.openecard.sc.iface.SecureMessagingException
import org.openecard.sc.iface.SmartCardStackMissing
import org.openecard.sc.iface.TerminalFactory
import org.openecard.sc.iface.feature.PaceError
import org.openecard.sc.iface.withContextSuspend
import org.openecard.sc.pace.PaceFeatureSoftwareFactory
import org.openecard.sc.tlv.toTlvSimple
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val MESSAGE_TIMEOUT_DURATION = 5.seconds

@OptIn(ExperimentalUnsignedTypes::class)
fun gunzip(data: UByteArray) =
    Buffer()
        .also {
            it.writeAll(
                GzipSource(
                    Buffer().also { b -> b.write(data.toByteArray()) },
                ),
            )
        }.readByteArray()

private val logger = KotlinLogging.logger { }

object CardlinkAuthenticationConfig {
    var readPersonalData = false
    var readInsurerData = false
}

class CardlinkAuthResult(
    var personalData: PersoenlicheVersichertendaten? = null,
    var insurerData: AllgemeineVersicherungsdaten? = null,
    var cardSessionId: String? = null,
    var iccsn: String? = null,
    var iccsnReassignmentTimestamp: String? = null,
    var wsSessionId: String? = null,
)

internal data class SessionInfo(
    val cardSessionId: String,
    val webSocketId: String? = null,
    val phoneRegistered: Boolean = false,
)

class CardlinkAuthenticationProtocol(
    private val terminalFactory: TerminalFactory,
    private val ws: Websocket,
) : CardLinkProtocolBase(ws) {
    private val inputChannel = protocolChannel<GematikEnvelope>()

    override fun messageHandler(msg: String): (suspend () -> Unit)? {
        try {
            val envelope = prescriptionJsonFormatter.decodeFromString<GematikEnvelope>(msg)
            return { inputChannel.send(envelope) }
        } catch (_: Exception) {
            return null
        }
    }

    internal lateinit var sessionInfo: SessionInfo
    private val cardLinkAuthResult = CardlinkAuthResult()
    lateinit var interaction: UserInteraction

    @Throws(
        CardlinkAuthenticationException::class,
        CardlinkAuthenticationClientException::class,
        CancellationException::class,
    )
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun establishCardlink(interaction: UserInteraction): CardlinkAuthResult =
        mapErrors {
            this.interaction = interaction
            ws.connectWithTimeout()

            sessionInfo =
                receiveSessionInformation().apply {
                    cardLinkAuthResult.cardSessionId = cardSessionId
                    cardLinkAuthResult.wsSessionId = webSocketId
                }

            if (!sessionInfo.phoneRegistered) {
                registerPhone()
            }

            retryIfPossible { lastCode, lastMsg ->
                val can = getCheckedCan(lastCode, lastMsg)
                withConnectedCard {
                    if (EgkCif.metadata.id != cardType) {
                        interaction.onCardInsufficient()
                        throw CardlinkAuthenticationClientException(
                            CardLinkErrorCodes.ClientCodes.OTHER_CLIENT_ERROR,
                            "Recognized card is not an eGK",
                        )
                    }

                    if (CardlinkAuthenticationConfig.readPersonalData) {
                        cardLinkAuthResult.personalData = readPersonalData(can) ?: readError("Personal data")
                    }
                    if (CardlinkAuthenticationConfig.readInsurerData) {
                        cardLinkAuthResult.insurerData = readInsurerData(can) ?: readError("Insurer data")
                    }

                    val readMfData =
                        readMfData(can)?.apply {
                            cardLinkAuthResult.iccsn = readIccsnFrom(gdo) ?: readError("ICCSN")
                        } ?: readError("MF")

                    val cert = readCert(can) ?: readError("Certificate")

                    sendEgkData(
                        sessionInfo.cardSessionId,
                        mfData = readMfData,
                        cert,
                    )

                    handleRemoteApdus()
                }
            }
            // no error means success
            cardLinkAuthResult
        }

    private fun readError(toBeRead: String): Nothing =
        throw CardlinkAuthenticationClientException(
            CardLinkErrorCodes.ClientCodes.OTHER_NFC_ERROR,
            "Could not read $toBeRead.",
        )

    private suspend fun mapErrors(block: suspend () -> CardlinkAuthResult): CardlinkAuthResult {
        try {
            return block()
        } catch (e: Exception) {
            when (e) {
                is CardlinkAuthenticationException,
                is CardlinkAuthenticationClientException,
                -> throw e
                is TimeoutCancellationException -> {
                    throw CardlinkAuthenticationClientException(
                        CardLinkErrorCodes.ClientCodes.CLIENT_TIMEOUT,
                        "Timeout during cardlink authentication",
                        e,
                    )
                }
                // wrong card
                is DeviceUnsupported -> {
                    interaction.onCardInsufficient()
                    throw CardlinkAuthenticationClientException(
                        CardLinkErrorCodes.ClientCodes.OTHER_NFC_ERROR,
                        "Card not usable.",
                    )
                }
                // appears if card gets lost
                is DeviceUnavailable -> {
                    interaction.onCardRemoved()
                    throw CardlinkAuthenticationClientException(
                        CardLinkErrorCodes.ClientCodes.CARD_REMOVED,
                        "Card connection lost.",
                    )
                }
                is CancellationException -> {
                    logger.warn(e) { "Cancellation appeared. Not throwing further." }
                    throw e
                }
                is SmartCardStackMissing -> {
                    throw CardlinkAuthenticationClientException(
                        CardLinkErrorCodes.ClientCodes.OTHER_NFC_ERROR,
                        "NFC not available",
                    )
                }
                else -> {
                    logger.error(e) { "Unknown exception" }
                    throw CardlinkAuthenticationClientException(
                        CardLinkErrorCodes.ClientCodes.OTHER_CLIENT_ERROR,
                        "${e.message}",
                        e,
                    )
                }
            }
        }
    }

    private suspend fun retryIfPossible(
        cardCommunication: suspend (
            lastResultCode: CardLinkErrorCodes.ClientCodes?,
            errorMessage: String?,
        ) -> Unit,
    ) {
        try {
            cardCommunication(null, null)
        } catch (e: CardlinkAuthenticationClientException) {
            // Errors where a retry makes sense can be added here
            when (e.code) {
                CardLinkErrorCodes.ClientCodes.CAN_INCORRECT -> {
                    retryIfPossible { _, _ ->
                        cardCommunication(e.code, e.message)
                    }
                }
                else -> {
                    throw e
                }
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun sendEgkData(
        cardSessionId: String,
        mfData: MFData,
        cert: UByteArray,
    ) {
        val registerEgkData =
            RegisterEgk(
                cardSessionId = cardSessionId,
                gdo = mfData.gdo!!.toByteArray(),
                cardVersion = mfData.cardVersion!!.toByteArray(),
                cvcAuth = mfData.cvcAuth!!.toByteArray(),
                cvcCA = mfData.cvcCA!!.toByteArray(),
                atr = mfData.atr!!.toByteArray(),
                x509AuthECC = cert.toByteArray(),
                x509AuthRSA = null,
            )
        sendEnvelope(registerEgkData)
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    private fun readIccsnFrom(gdoDs: UByteArray?) =
        gdoDs?.toTlvSimple()?.tlv.let {
            if (it?.tag?.tagNumWithClass == 0x5f5A.toULong()) {
                it.contentAsBytesBer.toHexString()
            } else {
                null
            }
        }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun sendEnvelope(
        payload: CardLinkPayload,
        correlationId: String? = Uuid.random().toString(),
    ) {
        ws.send(
            cardLinkJsonFormatter.encodeToString(
                GematikEnvelope(
                    payload,
                    correlationId,
                    sessionInfo.cardSessionId,
                ),
            ),
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
                        throw Exception("Correlation ids don't match")
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

    private suspend fun registerPhone() {
        requestTan()
        confirmTan()
    }

    private fun handleTaskListError(egkPayload: TasklistErrorPayload): Unit =
        throw CardlinkAuthenticationException(
            CardLinkErrorCodes.CardLinkCodes.byStatus(egkPayload.status)
                ?: CardLinkErrorCodes.CardLinkCodes.UNKNOWN_ERROR,
            egkPayload.errormessage ?: "Received an unknown error from CardLink service.",
        )

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
        sendEnvelope(
            SendPhoneNumber(
                number,
            ),
        )

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
        throw CardlinkAuthenticationException(
            CardLinkErrorCodes.CardLinkCodes.INVALID_WEBSOCKET_MESSAGE,
            errMsg,
        )
    }

    private fun handleInvalidResultCode(code: ResultCode?) {
        val errMsg = "Received invalid result code from service"
        logger.error { errMsg }
        logger.debug {
            "Result code was $code which is no valid answer at this step."
        }
        throw CardlinkAuthenticationException(
            CardLinkErrorCodes.CardLinkCodes.INVALID_WEBSOCKET_MESSAGE,
            errMsg,
        )
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
                        lastErrorMessage ?: "",
                    )
                }
            }

        sendEnvelope(
            SendTan(
                smsCode = userEnteredTan,
                tan = userEnteredTan,
            ),
        )
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
                    ResultCode.TAN_EXPIRED -> {
                        throw CardlinkAuthenticationException(
                            resultCode.toCardLinkErrorCode(),
                            egkPayload.errorMessage ?: "Tan expired.",
                        )
                    }
                    ResultCode.TAN_RETRY_LIMIT_EXCEEDED -> {
                        throw CardlinkAuthenticationException(
                            resultCode.toCardLinkErrorCode(),
                            egkPayload.errorMessage ?: "Tan retry limit exceeded.",
                        )
                    }

                    else -> handleInvalidResultCode(resultCode)
                }
            }

            else -> handleInvalidMessage(egkPayload)
        }
    }

    private suspend fun getCheckedCan(
        lastResultCode: CardLinkErrorCodes.ClientCodes? = null,
        errorMessage: String? = null,
    ): String {
        val can =
            if (lastResultCode == null) {
                interaction.onCanRequest()
            } else {
                interaction.onCanRetry(lastResultCode, errorMessage)
            }
        return if (can.isBlank()) {
            getCheckedCan(CardLinkErrorCodes.ClientCodes.CAN_EMPTY, "Empty CAN provided.")
        } else if (can.length > 6) {
            getCheckedCan(CardLinkErrorCodes.ClientCodes.CAN_TOO_LONG, "Wrong size of CAN: ${can.length}.")
        } else if (can.toIntOrNull() == null) {
            getCheckedCan(CardLinkErrorCodes.ClientCodes.CAN_NOT_NUMERIC, "Provided CAN is not numeric.")
        } else {
            can
        }
    }

    private suspend fun withConnectedCard(block: suspend SmartcardDeviceConnection.() -> Unit) {
        terminalFactory.load().withContextSuspend { terminals ->
            val recognition =
                DirectCardRecognition(CompleteTree.calls.removeUnsupported(setOf(EgkCifDefinitions.cardType)))

            val terminal =
                terminals.list().firstOrNull()
                    ?: throw CardlinkAuthenticationClientException(
                        CardLinkErrorCodes.ClientCodes.OTHER_NFC_ERROR,
                        "The nfc stack could not be initialized",
                    )

            val session =
                SmartcardSal(
                    terminals,
                    setOf(EgkCif),
                    recognition,
                    PaceFeatureSoftwareFactory(),
                ).startSession()
            interaction.requestCardInsertion()
            terminal.waitForCardPresent()
            interaction.onCardRecognized()
            session.connect(terminal.name).block()
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun SmartcardApplication.readDataSet(
        can: String,
        name: String,
    ): UByteArray? =
        datasets.find { it.name == name }?.run {
            authenticate(can)
            this@readDataSet.connect()
            read()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun SmartcardDeviceConnection.readPersonalData(can: String): PersoenlicheVersichertendaten? =
        applications.find { it.name == EgkCifDefinitions.Apps.Hca.name }?.run {
            readDataSet(can, EgkCifDefinitions.Apps.Hca.Datasets.efPd)?.toPersonalData()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun SmartcardDeviceConnection.readInsurerData(can: String): AllgemeineVersicherungsdaten? =
        applications.find { it.name == EgkCifDefinitions.Apps.Hca.name }?.run {
            readDataSet(can, EgkCifDefinitions.Apps.Hca.Datasets.efVd)?.toInsurerData()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    class MFData(
        val gdo: UByteArray? = null,
        val cardVersion: UByteArray? = null,
        val cvcAuth: UByteArray? = null,
        val cvcCA: UByteArray? = null,
        val atr: UByteArray? = null,
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun SmartcardDeviceConnection.readMfData(can: String) =
        applications.find { it.name == EgkCifDefinitions.Apps.Mf.name }?.run {
            MFData(
                gdo = readDataSet(can, EgkCifDefinitions.Apps.Mf.Datasets.efGdo),
                cardVersion = readDataSet(can, EgkCifDefinitions.Apps.Mf.Datasets.efVersion2),
                cvcAuth = readDataSet(can, EgkCifDefinitions.Apps.Mf.Datasets.ef_c_eGK_aut_cvc_e256),
                cvcCA = readDataSet(can, EgkCifDefinitions.Apps.Mf.Datasets.ef_c_ca_cs_e256),
                atr = readDataSet(can, EgkCifDefinitions.Apps.Mf.Datasets.efAtr),
            )
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun SmartcardDeviceConnection.readCert(can: String) =
        applications
            .find { it.name == EgkCifDefinitions.Apps.ESign.name }
            ?.readDataSet(can, EgkCifDefinitions.Apps.ESign.Datasets.ef_c_ch_aut_e256)

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun SmartcardDataset.authenticate(can: String) {
        if (!missingReadAuthentications.isSolved) {
            val missing =
                missingReadAuthentications
                    .removeUnsupported(
                        listOf(
                            DidStateReference.forName(EgkCifDefinitions.Apps.Mf.Dids.autPace),
                        ),
                    )
            when (missing) {
                MissingAuthentications.Unsolveable -> throw CardlinkAuthenticationClientException(
                    CardLinkErrorCodes.ClientCodes.OTHER_PACE_ERROR,
                    "Card authentication could not be performed.",
                )
                is MissingAuthentications.MissingDidAuthentications -> {
                    val authOption = missing.options.first()
                    when (val did = authOption.first().authDid) {
                        is PaceDid -> {
                            try {
                                did.establishChannel(can, null, null)
                            } catch (e: PaceError) {
                                throw CardlinkAuthenticationClientException(
                                    CardLinkErrorCodes.ClientCodes.CAN_INCORRECT,
                                    "The given CAN lead to error: ${e.message}",
                                )
                            }
                        }

                        else -> throw CardlinkAuthenticationClientException(
                            CardLinkErrorCodes.ClientCodes.OTHER_PACE_ERROR,
                            "Card authentication could not be performed.",
                        )
                    }
                }
            }
        }
    }

    private suspend fun SmartcardDeviceConnection.handleRemoteApdus() {
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
                sendEnvelope(
                    SendApduResponse(
                        payload.cardSessionId,
                        sendApduToCard(payload.apdu),
                    ),
                    correlationId = envelope.correlationId,
                )
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

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun SmartcardDeviceConnection.sendApduToCard(apdu: ByteArrayAsBase64): ByteArray {
        val cApdu = apdu.toCommandApdu()
        logger.debug { "APDU from service: $cApdu" }
        return try {
            val responseApdu = channel.transmit(cApdu)
            logger.debug { "Response APDU: $responseApdu" }
            responseApdu.toBytes.toByteArray()
        } catch (e: SecureMessagingException) {
            logger.debug(e) { "Error communicating with card." }
            throw e
        }
    }
}
