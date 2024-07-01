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

package com.epotheke.erezept.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


const val REQUEST_PRESCRIPTION_LIST = "requestPrescriptionList"
const val AVAILABLE_PRESCRIPTION_LISTS = "availablePrescriptionLists"
const val AVAILABLE_PRESCRIPTION_LIST = "availablePrescriptionList"
const val SELECTED_PRESCRIPTION_LIST = "selectedPrescriptionList"
const val GENERIC_ERROR = "genericError"
const val MEDICATION_PZN = "medicationPZN"
const val MEDICATION_FREE_TEXT = "medicationFreeText"
const val MEDICATION_COMPOUNDING = "medicationCompounding"
const val MEDICATION_INGREDIENT = "medicationIngredient"
const val BESTANDTEIL_WIRKSTOFF_VERORDNUNG= "bestandteilWirkstoffverordnung"
const val MEDICATION_SUMMARY = "medicationSummary"
const val CONFIRM_PRESCRIPTION_LIST = "confirmPrescriptionListMessage"


sealed interface ERezeptMessage

@Serializable
@SerialName(REQUEST_PRESCRIPTION_LIST)
data class RequestPrescriptionList(
    @SerialName("ICCSNs")
    val iccsns: List<ByteArrayAsBase64> = emptyList(),
    val messageId: String = UUID.randomUUID().toString(),
) : ERezeptMessage

@Serializable
@SerialName(AVAILABLE_PRESCRIPTION_LISTS)
data class AvailablePrescriptionLists(
    val availablePrescriptionLists: List<AvailablePrescriptionList>,
    val messageId: String = UUID.randomUUID().toString(),
    val correlationId: String,
) : ERezeptMessage

@Serializable
@SerialName(SELECTED_PRESCRIPTION_LIST)
data class SelectedPrescriptionList(
    @SerialName("ICCSN")
    val iccsn: ByteArrayAsBase64,
    val medicationIndexList: List<Int>,
    val supplyOptionsType: SupplyOptionsType,
    val name: String? = null,
    val address: List<String>? = null,
    val hint: String? = null,
    val phone: String? = null,
    val messageId: String = UUID.randomUUID().toString(),
) : ERezeptMessage

@Serializable
@SerialName(GENERIC_ERROR)
data class GenericErrorMessage(
    val errorCode: GenericErrorResultType,
    val errorMessage: String,
    val messageId: String = UUID.randomUUID().toString(),
    val correlationId: String? = null,
) : ERezeptMessage

@Serializable
@SerialName(CONFIRM_PRESCRIPTION_LIST)
data class ConfirmPrescriptionList(
    val messageId: String = UUID.randomUUID().toString(),
    val correlationId: String,
) : ERezeptMessage

@Serializable
@SerialName(AVAILABLE_PRESCRIPTION_LIST)
data class AvailablePrescriptionList(
    @SerialName("ICCSN")
    val iccsn: ByteArrayAsBase64,
    val medicationSummaryList: List<MedicationSummary>,
    val hint: String? = null,
)

@Serializable
@SerialName(BESTANDTEIL_WIRKSTOFF_VERORDNUNG)
data class BestandteilWirkstoffverordnung(
    val wirkstoffnummer: String,
    val wirkstoffname: String,
    val wirkstaerke: String,
    val wirkstaerkeneinheit: String,
)

enum class GenericErrorResultType(val value: String) {
    INVALID_MESSAGE_DATA("INVALID_MESSAGE_DATA"),
    TI_UNAVAILABLE("TI_UNAVAILABLE"),
    TI_SERVICE_ERROR("TI_SERVICE_ERROR"),
    CARD_EXPIRED("CARD_EXPIRED"),
    CARD_REVOKED("CARD_REVOKED"),
    CARD_INVALID("CARD_INVALID"),
    CARD_ERROR("CARD_ERROR"),
    UNKNOWN_ERROR("UNKNOWN_ERROR");
}

sealed interface Medication

@Serializable
@SerialName(MEDICATION_COMPOUNDING)
data class MedicationCompounding(
    val kategorie: String,
    val impfstoff: Boolean,
    val herstellungsanweisung: String,
    val verpackung: String? = null,
    val rezepturname: String,
    val darreichungsform: String,
    val gesamtmenge: String,
    val einheit: String? = null,
    val listeBestandteilRezepturverordnung: List<BestandteilWirkstoffverordnung>,
) : Medication

@Serializable
@SerialName(MEDICATION_FREE_TEXT)
data class MedicationFreeText(
    val kategorie: String,
    val impfstoff: Boolean,
    val freitextverordnung: String,
    val darreichungsform: String,
) : Medication

@Serializable
@SerialName(MEDICATION_INGREDIENT)
data class MedicationIngredient(
    val kategorie: String,
    val impfstoff: Boolean,
    val normgroesse: String? = null,
    val darreichungsform: String,
    val packungsgroesseNachMenge: String? = null,
    val einheit: String? = null,
    val packungsgroesseNachNBezeichnung: String? = null,
    val listeBestandteilWirkstoffverordnung: List<BestandteilWirkstoffverordnung>,
) : Medication

@Serializable
@SerialName(MEDICATION_PZN)
data class MedicationPzn(
    val kategorie: String,
    val impfstoff: Boolean,
    val normgroesse: String? = null,
    val pzn: String,
    val handelsname: String,
    val darreichungsform: String,
    val packungsgroesseNachMenge: String? = null,
    val einheit: String? = null,
    val packungsgroesseNachNBezeichnung: String? = null,
) : Medication

@Serializable
@SerialName(MEDICATION_SUMMARY)
data class MedicationSummary(
    val index: Int,
    val medication: Medication,
)

enum class SupplyOptionsType {
    @SerialName("onPremise")
    ON_PREMISE,
    @SerialName("shipment")
    SHIPMENT,
    @SerialName("delivery")
    DELIVERY;
}

val eRezeptModule = SerializersModule {
    polymorphic(ERezeptMessage::class) {
        subclass(RequestPrescriptionList::class)
        subclass(AvailablePrescriptionLists::class)
        subclass(SelectedPrescriptionList::class)
        subclass(ConfirmPrescriptionList::class)
        subclass(GenericErrorMessage::class)
    }
    polymorphic(Medication::class) {
        subclass(MedicationCompounding::class)
        subclass(MedicationIngredient::class)
        subclass(MedicationFreeText::class)
        subclass(MedicationPzn::class)
    }
}

val eRezeptJsonFormatter = Json {
    serializersModule = eRezeptModule;
    classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS;
    ignoreUnknownKeys = true
}

typealias ByteArrayAsBase64 = @Serializable(ByteArrayAsBase64Serializer::class) ByteArray

@OptIn(ExperimentalEncodingApi::class)
object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ByteArrayAsBase64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64Encoded = Base64.encode(value).trimEnd('=')
        encoder.encodeString(base64Encoded)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        return Base64.decode(decoder.decodeString())
    }
}
