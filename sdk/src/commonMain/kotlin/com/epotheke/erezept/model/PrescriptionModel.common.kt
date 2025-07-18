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

import kotlinx.serialization.ExperimentalSerializationApi
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
import com.epotheke.sdk.randomUUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

const val REQUEST_PRESCRIPTION_LIST = "requestPrescriptionList"
const val AVAILABLE_PRESCRIPTION_LISTS = "availablePrescriptionLists"
const val AVAILABLE_PRESCRIPTION_LIST = "availablePrescriptionList"
const val SELECTED_PRESCRIPTION_LIST = "selectedPrescriptionList"
const val PRESCRIPTION_FETCH_ERROR = "prescriptionFetchError"
const val GENERIC_ERROR = "genericError"
const val MEDICATION_PZN = "medicationPZN"
const val MEDICATION_FREE_TEXT = "medicationFreeText"
const val MEDICATION_COMPOUNDING = "medicationCompounding"
const val MEDICATION_INGREDIENT = "medicationIngredient"
const val BESTANDTEIL_WIRKSTOFF_VERORDNUNG= "bestandteilWirkstoffverordnung"
const val BESTANDTEIL_REZEPTUR_VERORDNUNG = "bestandteilRezepturverordnung"
const val SELECTED_PRESCRIPTION_LIST_RESPONSE = "selectedPrescriptionListResponse"
const val PRESCRIPTION_BUNDLE = "prescriptionBundle"
const val MEDICATION = "medication"
const val ORGANIZATION = "organization"
const val PATIENT = "patient"
const val PERSON = "person"
const val STREET_ADDRESS = "streetAddress"
const val POB_ADDRESS = "pobAddress"
const val PRACTICE_SUPPLY = "practiceSupply"
const val PRACTITIONER = "practitioner"
const val PRESCRIPTION = "prescription"
const val COVERAGE = "coverage"

@Serializable
sealed interface PrescriptionMessage

@Serializable
@SerialName(REQUEST_PRESCRIPTION_LIST)
data class RequestPrescriptionList(
    @SerialName("ICCSNs")
    val iccsns: List<ByteArrayAsBase64> = emptyList(),
    val messageId: String = randomUUID(),
) : PrescriptionMessage

@Serializable
@SerialName(AVAILABLE_PRESCRIPTION_LISTS)
data class AvailablePrescriptionLists(
    val availablePrescriptionLists: List<AvailablePrescriptionList>,
    val messageId: String = randomUUID(),
    val correlationId: String,
) : PrescriptionMessage

@Serializable
@SerialName(SELECTED_PRESCRIPTION_LIST)
data class SelectedPrescriptionList(
    @SerialName("ICCSN")
    val iccsn: ByteArrayAsBase64,
    val prescriptionIndexList: List<String>,
    val version: String? = null,
    val supplyOptionsType: SupplyOptionsType,
    val name: String? = null,
    val address: StreetAddress? = null,
    val hint: String? = null,
    val text: String? = null,
    val phone: String? = null,
    val mail: String? = null,
    val messageId: String = randomUUID(),
) : PrescriptionMessage

@Serializable
@SerialName(GENERIC_ERROR)
data class GenericErrorMessage(
    val errorCode: GenericErrorResultType,
    val errorMessage: String,
    val messageId: String = randomUUID(),
    val correlationId: String? = null,
) : PrescriptionMessage

@Serializable
@SerialName(SELECTED_PRESCRIPTION_LIST_RESPONSE)
data class SelectedPrescriptionListResponse(
    val version: String? = null,
    val supplyOptionsType: SupplyOptionsType? = null,
    val infoText: String? = null,
    val url: String? = null,
    val pickUpCodeHr: String? = null,
    val pickUpCodeDmc: String? = null,
    val messageId: String,
    val correlationId: String,
) : PrescriptionMessage

@Serializable
@SerialName(AVAILABLE_PRESCRIPTION_LIST)
data class AvailablePrescriptionList(
    @SerialName("ICCSN")
    val iccsn: ByteArrayAsBase64,
    val prescriptionBundleList: List<PrescriptionBundle>,
    val fetchErrors: List<PrescriptionFetchError>? = null,
)

@Serializable
@SerialName(PRESCRIPTION_FETCH_ERROR)
data class PrescriptionFetchError(
    val prescriptionId: String,
    val errorCode: PrescriptionFetchErrorCode,
    val errorMessage: String,
)

enum class PrescriptionFetchErrorCode(
    val value: String,
) {
    TI_FORBIDDEN("TI_FORBIDDEN"),
    TI_PRESCRIPTION_NOT_FOUND("TI_PRESCRIPTION_NOT_FOUND"),
    TI_TOO_MANY_REQUESTS_SENT("TI_TOO_MANY_REQUESTS_SENT"),
    TI_INTERNAL_SERVER_ERROR("TI_INTERNAL_SERVER_ERROR"),
    TI_UNKNOWN_ERROR("TI_UNKNOWN_ERROR"),
    CONNECT_TIMEOUT_ERROR("CONNECT_TIMEOUT_ERROR"),
    SERIALIZATION_ERROR("SERIALIZATION_ERROR"),
    UNKNOWN_ERROR("UNKNOWN_ERROR"),
}

@Serializable
@SerialName(PRESCRIPTION_BUNDLE)
data class PrescriptionBundle(
    val prescriptionId: String,
    val accessCode: String? = null,
    val erstellungszeitpunkt: String,
    val status: String,
    val krankenversicherung: Coverage?,
    val pkvTarif: String? = null,
    val patient: Patient?,
    val arzt: Practitioner,
    val pruefnummer: String? = null,
    val organisation: Organization,
    val asvTn: String? = null,
    val verordnung: PrescriptionInterface?,
    val arzneimittel: Medication,
)

@Serializable
@SerialName(COVERAGE)
data class Coverage(
    val kostentraegertyp: String,
    val ikKrankenkasse: String,
    val ikKostentraeger: String? = null,
    val kostentraeger: String? = null,
    val wop: String? = null,
    val versichertenstatus: String? = null,
    val besonderePersonengruppe: String? = null,
    val dmpKz: String? = null,
    val versicherungsschutzEnde: String? = null,
)

@Serializable
@SerialName(BESTANDTEIL_WIRKSTOFF_VERORDNUNG)
data class BestandteilWirkstoffverordnung(
    val wirkstoffnummer: String,
    val wirkstoffname: String,
    val wirkstaerke: String,
    val wirkstaerkeneinheit: String,
)

@Serializable
@SerialName(BESTANDTEIL_REZEPTUR_VERORDNUNG)
data class BestandteilRezepturverordnung(
    val darreichungsform: String? = null,
    val name: String,
    val pzn: String? = null,
    val menge: String? = null,
    val einheit: String? = null,
    val mengeUndEinheit: String? = null,
)

enum class GenericErrorResultType(val value: String) {
    INVALID_MESSAGE_DATA("INVALID_MESSAGE_DATA"),
    TI_UNAVAILABLE("TI_UNAVAILABLE"),
    TI_SERVICE_ERROR("TI_SERVICE_ERROR"),
    CARD_EXPIRED("CARD_EXPIRED"),
    CARD_REVOKED("CARD_REVOKED"),
    CARD_INVALID("CARD_INVALID"),
    CARD_ERROR("CARD_ERROR"),
    UNSUPPORTED_ENVELOPE("UNSUPPORTED_ENVELOPE"),
    NO_PRESCRIPTIONS_AVAILABLE("NO_PRESCRIPTIONS_AVAILABLE"),
    NOT_FOUND("NOT_FOUND"),
    UNKNOWN_ERROR("UNKNOWN_ERROR");
}

@Serializable
sealed interface MedicationItem

@Serializable
@SerialName(MEDICATION_COMPOUNDING)
data class MedicationCompounding(
    val kategorie: String,
    val impfstoff: Boolean,
    val herstellungsanweisung: String? = null,
    val verpackung: String? = null,
    val rezepturname: String? = null,
    val darreichungsform: String? = null,
    val gesamtmenge: String,
    val einheit: String? = null,
    val listeBestandteilRezepturverordnung: List<BestandteilRezepturverordnung>,
) : MedicationItem

@Serializable
@SerialName(MEDICATION_FREE_TEXT)
data class MedicationFreeText(
    val kategorie: String,
    val impfstoff: Boolean,
    val freitextverordnung: String,
    val darreichungsform: String? = null,
) : MedicationItem

@Serializable
@SerialName(MEDICATION_INGREDIENT)
data class MedicationIngredient(
    val kategorie: String,
    val impfstoff: Boolean,
    val normgroesse: String? = null,
    val darreichungsform: String? = null,
    val packungsgroesseNachMenge: String? = null,
    val einheit: String? = null,
    val packungsgroesseNachNBezeichnung: String? = null,
    val listeBestandteilWirkstoffverordnung: List<BestandteilWirkstoffverordnung>,
) : MedicationItem

@Serializable
@SerialName(MEDICATION_PZN)
data class MedicationPzn(
    val kategorie: String,
    val impfstoff: Boolean,
    val normgroesse: String? = null,
    val pzn: String,
    val handelsname: String,
    val darreichungsform: String? = null,
    val packungsgroesseNachMenge: String? = null,
    val einheit: String? = null,
    val packungsgroesseNachNBezeichnung: String? = null,
) : MedicationItem

@Serializable
@SerialName(MEDICATION)
data class Medication(
    val medicationItem: List<MedicationItem>,
)

@Serializable
@SerialName(ORGANIZATION)
data class Organization(
    val bsnr: String? = null,
    val ikNummer: String? = null,
    val kzvAn: String? = null,
    val standortnummer: String? = null,
    val telematikId: String? = null,
    val name: String? = null,
    val address: StreetAddress? = null,
    val telefon: String? = null,
    val fax: String? = null,
    val eMail: String? = null,
)

@Serializable
@SerialName(PATIENT)
data class Patient(
    val gkvVersichertenId: String? = null,
    val pkvVersichertenId: String? = null,
    val kvkVersichertennummer: String? = null,
    val person: Person,
    val geburtsdatum: String,
    val adresse: Address? = null,
)

@Serializable
@SerialName(PERSON)
data class Person(
    val vorname: String,
    val name: String? = null,
    val titel: String? = null,
    val namenszusatz: String? = null,
    val vorsatzwort: String? = null,
)

@Serializable
sealed interface Address

@Serializable
@SerialName(STREET_ADDRESS)
data class StreetAddress(
    val land: String? = null,
    val plz: String,
    val ort: String,
    val strasse: String,
    val hausnummer: String? = null,
    val zusatz: String? = null,
) : Address

@Serializable
@SerialName(POB_ADDRESS)
data class PobAddress(
    val land: String? = null,
    val plz: String,
    val ort: String,
    val postfach: String,
) : Address

@Serializable
sealed interface PrescriptionInterface

@Serializable
@SerialName(PRACTICE_SUPPLY)
data class PracticeSupply(
    val datum: String,
    val anzahl: Int,
    val anzahlEinheit: String? = null,
    val kostentraegertyp: String,
    val ikNummer: String,
    val name: String,
) : PrescriptionInterface

@Serializable
@SerialName(PRESCRIPTION)
data class Prescription(
    val ausstellungsdatum: String? = null,
    val noctu: Boolean? = null,
    val serKennzeichen: Boolean? = null,
    val bvg: Boolean? = null,
    val verschreiberID: String? = null,
    val zuzahlungsstatus: String? = null,
    val autidem: Boolean? = null,
    val abgabehinweis: String? = null,
    val anzahl: Int? = null,
    val anzahlEinheit: String? = null,
    val dosierung: Boolean? = null,
    val dosieranweisung: String? = null,
    val gebrauchsanweisung: String? = null,
    val unfallkennzeichen: String? = null,
    val unfalltag: String? = null,
    val unfallbetrieb: String? = null,
    val mehrfachverordnung: Boolean? = null,
    val mfvId: String? = null,
    val mfvZaehler: Int? = null,
    val mfvNenner: Int? = null,
    val mfvBeginn: String? = null,
    val mfvEnde: String? = null,
) : PrescriptionInterface

@Serializable
@SerialName(PRACTITIONER)
data class Practitioner(
    val typ: String,
    val berufsbezeichnung: String? = null,
    val asvFgn: String? = null,
    val arztnummer: String? = null,
    val zahnarztnummer: String? = null,
    val telematikId: String? = null,
    val person: Person,
    val verantwortlichePerson: Practitioner? = null,
)

enum class SupplyOptionsType {
    @SerialName("onPremise")
    ON_PREMISE,
    @SerialName("shipment")
    SHIPMENT,
    @SerialName("delivery")
    DELIVERY;
}

private val prescriptionModule = SerializersModule {
    polymorphic(PrescriptionMessage::class) {
        subclass(RequestPrescriptionList::class)
        subclass(AvailablePrescriptionLists::class)
        subclass(SelectedPrescriptionList::class)
        subclass(SelectedPrescriptionListResponse::class)
        subclass(GenericErrorMessage::class)
    }
    polymorphic(MedicationItem::class) {
        subclass(MedicationCompounding::class)
        subclass(MedicationIngredient::class)
        subclass(MedicationFreeText::class)
        subclass(MedicationPzn::class)
    }
    polymorphic(PrescriptionInterface::class) {
        subclass(PracticeSupply::class)
        subclass(Prescription::class)
    }
    polymorphic(Address::class) {
        subclass(StreetAddress::class)
        subclass(PobAddress::class)
    }
}


@OptIn(ExperimentalSerializationApi::class)
val prescriptionJsonFormatter = Json {
    serializersModule = prescriptionModule
    classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS
    ignoreUnknownKeys = true
    // convert missing values to null and do not emit null values
    explicitNulls = false
    encodeDefaults = true
}

typealias ByteArrayAsBase64 = @Serializable(ByteArrayAsBase64Serializer::class) ByteArray

@OptIn(ExperimentalEncodingApi::class)
object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ByteArrayAsBase64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(
            Base64.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).encode(value)
        )
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        return Base64.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(decoder.decodeString())
    }
}
