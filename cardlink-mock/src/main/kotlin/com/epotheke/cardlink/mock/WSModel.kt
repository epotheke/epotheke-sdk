/****************************************************************************
 * Copyright (C) 2024 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the CardLink-Service.
 *
 ***************************************************************************/

package com.epotheke.cardlink.mock


import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

const val READY = "ready"
const val REGISTER_EGK = "registerEGK"
const val SEND_APDU = "sendAPDU"
const val SEND_APDU_RESPONSE = "sendAPDUResponse"

const val TASK_LIST_ERROR = "receiveTasklistError"

// Newly defined types
const val REQUEST_SMS_TAN = "requestSMSCode"
const val REQUEST_SMS_TAN_RESPONSE = "requestSMSCodeResponse"
const val CONFIRM_TAN = "confirmSMSCode"
const val CONFIRM_TAN_RESPONSE = "confirmSMSCodeResponse"

// additional types for the base specification
const val SESSION_INFO = "sessionInformation"
const val REGISTER_EGK_FINISH = "registerEgkFinish"
const val ICCSN_REASSIGNMENT = "ICCSNReassignment"

@Serializable(with = GematikMessageSerializer::class)
data class GematikEnvelope(
    val payload: CardLinkPayload?,
    val correlationId: String?,
    val cardSessionId: String?,
)

object GematikMessageSerializer : KSerializer<GematikEnvelope> {
    // Not really used, but must be implemented
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("GematikMessage") {
            element<String>("cardSessionId")
            element<String>("correlationId")
            element<CardLinkPayload>("payload")
            element<String>("payloadType")
        }

    @OptIn(ExperimentalEncodingApi::class)
    override fun serialize(
        encoder: Encoder,
        value: GematikEnvelope,
    ) {
        val payloadType =
            value.payload?.let {
                it::class.java.getAnnotation(SerialName::class.java)?.value
            }
        val base64EncodedPayload: String =
            value.payload.let {
                val payloadJsonStr = cardLinkJsonFormatter.encodeToString(it)
                Base64
                    .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                    .encode(payloadJsonStr.encodeToByteArray())
            }
        val jsonPayload =
            buildJsonObject {
                put("type", payloadType)
                put("payload", base64EncodedPayload)
            }
        val jsonElement =
            buildJsonArray {
                add(jsonPayload)
                value.cardSessionId?.let { add(it) }
                value.correlationId?.let { add(it) }
            }
        encoder.encodeSerializableValue(JsonElement.serializer(), jsonElement)
    }

    override fun deserialize(decoder: Decoder): GematikEnvelope {
        val websocketMessage = decoder.decodeSerializableValue(JsonElement.serializer())

        val gematikMessage =
            websocketMessage.jsonArray.getOrNull(0)?.jsonObject
                ?: throw IllegalArgumentException("Web-Socket Gematik message does not contain a payload.")
        val cardSessionId =
            websocketMessage.jsonArray
                .getOrNull(1)
                ?.jsonPrimitive
                ?.content
        val correlationId =
            websocketMessage.jsonArray
                .getOrNull(2)
                ?.jsonPrimitive
                ?.content

        val payloadType =
            gematikMessage["type"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Web-Socket Gematik message does not contain a type.")
        val payload =
            gematikMessage["payload"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Web-Socket Gematik message does not contain a payload value.")

        val typedJsonElement = toTypedJsonElement(payload, payloadType)
        val cardLinkPayload = cardLinkJsonFormatter.decodeFromJsonElement<CardLinkPayload>(typedJsonElement)

        return GematikEnvelope(cardLinkPayload, correlationId, cardSessionId)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun toTypedJsonElement(
        base64EncodedPayload: String,
        payloadType: String,
    ): JsonObject {
        val jsonPayload =
            Base64
                .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                .decode(base64EncodedPayload)
                .toString(Charsets.UTF_8)
        val jsonElement = Json.parseToJsonElement(jsonPayload)
        return JsonObject(
            jsonElement.jsonObject.toMutableMap().apply {
                put(Json.configuration.classDiscriminator, JsonPrimitive(payloadType))
            },
        )
    }
}

val gematikJsonModule =
    SerializersModule {
        contextual(GematikEnvelope.serializer())
        contextual(ByteArrayOfHexAsBase64Serializer)
        polymorphic(CardLinkPayload::class) {
            subclass(RegisterEgk::class)
            subclass(SendApdu::class)
            subclass(SendApduResponse::class)
            subclass(SendPhoneNumber::class)
            subclass(SendTan::class)
            subclass(ConfirmTan::class)
            subclass(ConfirmPhoneNumber::class)
            subclass(RegisterEgkFinish::class)
            subclass(SessionInformation::class)
            subclass(TasklistErrorPayload::class)
            subclass(ICCSNReassignment::class)
        }
    }

val gematikDebugModule =
    SerializersModule {
        contextual(GematikEnvelope.serializer())
        contextual(ByteArrayOfHexAsHexSerializer)
        polymorphic(CardLinkPayload::class) {
            subclass(RegisterEgk::class)
            subclass(SendApdu::class)
            subclass(SendApduResponse::class)
            subclass(SendPhoneNumber::class)
            subclass(SendTan::class)
            subclass(ConfirmTan::class)
            subclass(ConfirmPhoneNumber::class)
            subclass(RegisterEgkFinish::class)
            subclass(SessionInformation::class)
            subclass(TasklistErrorPayload::class)
            subclass(ICCSNReassignment::class)
        }
    }

@OptIn(ExperimentalSerializationApi::class)
val cardLinkJsonFormatter =
    Json {
        serializersModule = gematikJsonModule
        classDiscriminatorMode =
            ClassDiscriminatorMode.NONE
        ignoreUnknownKeys = true
    }

@OptIn(ExperimentalSerializationApi::class)
val cardLinkDebugFormatter =
    Json {
        serializersModule = gematikDebugModule
        classDiscriminatorMode =
            ClassDiscriminatorMode.NONE
        ignoreUnknownKeys = true
    }

sealed interface CardLinkPayload

@Serializable
@SerialName(SESSION_INFO)
data class SessionInformation(
    val webSocketId: String,
    val phoneRegistered: Boolean,
) : CardLinkPayload

@Serializable
@SerialName(REGISTER_EGK)
data class RegisterEgk(
    val cardSessionId: String,
    val gdo: ByteArrayAsBase64,
    val cardVersion: ByteArrayAsBase64,
    val x509AuthRSA: ByteArrayAsBase64? = null,
    val x509AuthECC: ByteArrayAsBase64,
    val cvcAuth: ByteArrayAsBase64,
    val cvcCA: ByteArrayAsBase64,
    val atr: ByteArrayAsBase64,
) : CardLinkPayload

@Serializable
@SerialName(SEND_APDU)
data class SendApdu(
    val cardSessionId: String,
    @Contextual
    val apdu: ByteArrayOfHex,
) : CardLinkPayload

@Serializable
@SerialName(SEND_APDU_RESPONSE)
data class SendApduResponse(
    val cardSessionId: String,
    @Contextual
    val response: ByteArrayOfHex,
) : CardLinkPayload

@Serializable
@SerialName(TASK_LIST_ERROR)
data class TasklistErrorPayload(
    val cardSessionId: String,
    val status: Int,
    val tistatus: String? = null,
    val rootcause: String? = null,
    val errormessage: String? = null,
) : CardLinkPayload

@Serializable
@SerialName(REQUEST_SMS_TAN)
data class SendPhoneNumber(
    val phoneNumber: String,
) : CardLinkPayload

@Serializable
@SerialName(REQUEST_SMS_TAN_RESPONSE)
data class ConfirmPhoneNumber(
    var resultCode: ResultCode,
    var errorMessage: String?,
) : CardLinkPayload

@Serializable
@SerialName(CONFIRM_TAN)
data class SendTan(
    val smsCode: String,
) : CardLinkPayload

@Serializable
@SerialName(CONFIRM_TAN_RESPONSE)
data class ConfirmTan(
    var resultCode: ResultCode,
    var errorMessage: String?,
) : CardLinkPayload

@Serializable
@SerialName(ICCSN_REASSIGNMENT)
data class ICCSNReassignment(
    val lastAssignment: InstantSerde,
) : CardLinkPayload

@Serializable
enum class ResultCode {
    SUCCESS,
    NUMBER_FROM_WRONG_COUNTRY,
    NUMBER_BLOCKED,
    TAN_EXPIRED,
    TAN_INCORRECT,
    TAN_RETRY_LIMIT_EXCEEDED,
    INVALID_REQUEST,
    UNKNOWN_ERROR,
}

@Serializable
@SerialName(REGISTER_EGK_FINISH)
data class RegisterEgkFinish(
    val removeCard: Boolean,
) : CardLinkPayload
