package com.epotheke.cardlink

sealed class CardLinkError(
    override val message: String,
    val code: Int? = null,
) : Exception() {
    companion object {
        fun byCode(
            code: Int?,
            message: String? = null,
        ): CardLinkError =
            when (code) {
                1004 -> NotFound(message)
                1005 -> SicctError(message)
                1006 -> ProcessAlreadyStarted(message)
                1007 -> UnknownWebsocketMessage(message)
                1008 -> InvalidWebsocketMessage(message)
                1009 -> EgkLimitReached(message)
                1010 -> SessionExpired(message)
                1011 -> ExpiredCertificate(message)
                1012 -> InvalidCertificate(message)
                1013 -> CertificateValidityMismatch(message)
                1014 -> InvalidGdo(message)
                1015 -> IccsnMismatch(message)
                1016 -> InvalidEfAtr(message)
                1017 -> UnableToSendSms(message)
                1018 -> NotAdmissibleTelPrefix(message)
                1019 -> UnknownError(message)
                1022 -> TanExpired(message)
                1024 -> TanRetryLimitExceeded(message)
                1025 -> ServerTimeout(message)
                else -> UnknownError(message)
            }
    }
}

class CorrelationIdMismatch(
    message: String? = null,
) : CardLinkError(message ?: "CorrelationId of received message did not match.")

class NotFound(
    message: String? = null,
) : CardLinkError(message ?: "Requested Entity Not found", 1004)

class SicctError(
    message: String? = null,
) : CardLinkError(message ?: "SICCT service returns an error", 1005)

class ProcessAlreadyStarted(
    message: String? = null,
) : CardLinkError(message ?: "Register eGK process is already ongoing", 1006)

class UnknownWebsocketMessage(
    message: String? = null,
) : CardLinkError(message ?: "Unknown Web Socket message", 1007)

class InvalidWebsocketMessage(
    message: String? = null,
) : CardLinkError(
        message
            ?: "Invalid Web Socket message, can occur if required data are missing or message encoding is wrong",
        1008,
    )

class EgkLimitReached(
    message: String? = null,
) : CardLinkError(message ?: "Limit of 10 eGKs per session reached", 1009)

class SessionExpired(
    message: String? = null,
) : CardLinkError(message ?: "session time has exceeded the permissible 15 minutes", 1010)

class ExpiredCertificate(
    message: String? = null,
) : CardLinkError(message ?: "Expired eGK certificate", 1011)

class InvalidCertificate(
    message: String? = null,
) : CardLinkError(message ?: "Invalid eGK certificate (signature invalid, not a valid eGK certificate, ...)", 1012)

class CertificateValidityMismatch(
    message: String? = null,
) : CardLinkError(message ?: "Mismatch between certificate validity periods of X.509 and CVC", 1013)

class InvalidGdo(
    message: String? = null,
) : CardLinkError(message ?: "Invalid EF.GDO", 1014)

class IccsnMismatch(
    message: String? = null,
) : CardLinkError(message ?: "Mismatch between ICCSN in CV certificate and EF.GDO", 1015)

class InvalidEfAtr(
    message: String? = null,
) : CardLinkError(message ?: "Invalid EF.ATR", 1016)

class UnableToSendSms(
    message: String? = null,
) : CardLinkError(message ?: "Unable to send SMS for Tan validation", 1017)

class NotAdmissibleTelPrefix(
    message: String? = null,
) : CardLinkError(message ?: "Not admissible telephone number prefix, only +49... is allowed", 1018)

class UnknownError(
    message: String? = null,
) : CardLinkError(
        message ?: "Unknown error, probably an internal server error happened or used on an unknown result code",
        1019,
    )

class TanExpired(
    message: String? = null,
) : CardLinkError(message ?: "Tan has expired", 1022)

class TanRetryLimitExceeded(
    message: String? = null,
) : CardLinkError(message ?: "Tan retry limit exceeded", 1024)

class ServerTimeout(
    message: String? = null,
) : CardLinkError(message ?: "If the client does not receive an APDU message from the CardLink service", 1025)
