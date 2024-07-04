package com.epotheke.cardlink.mock

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.*


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
    val prescriptionIndexList: List<String>,
    val version: String? = null,
    val supplyOptionsType: SupplyOptionsType,
    val name: String? = null,
    val address: StreetAddress? = null,
    val hint: String? = null,
    val text: String? = null,
    val phone: String? = null,
    val mail: String? = null,
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
) : ERezeptMessage

@Serializable
@SerialName(AVAILABLE_PRESCRIPTION_LIST)
data class AvailablePrescriptionList(
    @SerialName("ICCSN")
    val iccsn: ByteArrayAsBase64,
    val prescriptionBundleList: List<PrescriptionBundle>,
)

@Serializable
@SerialName(PRESCRIPTION_BUNDLE)
data class PrescriptionBundle(
    val prescriptionId: String,
    val erstellungszeitpunkt: String,
    val status: String,
    val krankenversicherung: Coverage,
    val pkvTarif: String? = null,
    val patient: Patient,
    val arzt: Practitioner,
    val pruefnummer: String? = null,
    val organisation: Organization,
    val asvTn: String? = null,
    val verordnung: PrescriptionInterface,
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
    val darreichungsform: String,
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
    UNKNOWN_ERROR("UNKNOWN_ERROR");
}

sealed interface MedicationItem

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
    val listeBestandteilRezepturverordnung: List<BestandteilRezepturverordnung>,
) : MedicationItem

@Serializable
@SerialName(MEDICATION_FREE_TEXT)
data class MedicationFreeText(
    val kategorie: String,
    val impfstoff: Boolean,
    val freitextverordnung: String,
    val darreichungsform: String,
) : MedicationItem

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
) : MedicationItem

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

sealed interface Address

@Serializable
@SerialName(STREET_ADDRESS)
data class StreetAddress(
    val land: String? = null,
    val plz: String,
    val ort: String,
    val strasse: String,
    val hausnummer: String,
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

sealed interface PrescriptionInterface

@Serializable
@SerialName(PRACTICE_SUPPLY)
data class PracticeSupply(
    val datum: String,
    val anzahl: Int,
    val kostentraegertyp: String,
    val ikNummer: String,
    val name: String,
) : PrescriptionInterface

@Serializable
@SerialName(PRESCRIPTION)
data class Prescription(
    val ausstellungsdatum: String? = null,
    val noctu: Boolean? = null,
    val bvg: Boolean? = null,
    val zuzahlungsstatus: String? = null,
    val autidem: Boolean? = null,
    val abgabehinweis: String? = null,
    val anzahl: Int? = null,
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

val eRezeptModule = SerializersModule {
    polymorphic(ERezeptMessage::class) {
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

val eRezeptJsonFormatter = Json {
    serializersModule = eRezeptModule;
    classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS;
    ignoreUnknownKeys = true
}
