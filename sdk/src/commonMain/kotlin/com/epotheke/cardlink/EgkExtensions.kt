package com.epotheke.cardlink

import com.epotheke.cardlink.core.CardInsufficient
import com.epotheke.cardlink.core.Egk
import com.epotheke.cardlink.data.AllgemeineVersicherungsdaten
import com.epotheke.cardlink.data.PersoenlicheVersichertendaten
import com.epotheke.cardlink.data.toInsurerData
import com.epotheke.cardlink.data.toPersonalData
import org.openecard.cif.bundled.EgkCifDefinitions
import org.openecard.sc.tlv.toTlvSimple

@OptIn(ExperimentalUnsignedTypes::class)
class MFData(
    val gdo: UByteArray,
    val cardVersion: UByteArray,
    val cvcAuth: UByteArray,
    val cvcCA: UByteArray,
    val atr: UByteArray,
)

@OptIn(ExperimentalUnsignedTypes::class)
val Egk.personalData: PersoenlicheVersichertendaten
    get() {
        return personalDataBytes.toPersonalData() ?: readError(EgkCifDefinitions.Apps.Hca.Datasets.efPd)
    }

@OptIn(ExperimentalUnsignedTypes::class)
val Egk.insurerData: AllgemeineVersicherungsdaten
    get() {
        return insurerDataBytes.toInsurerData() ?: readError(EgkCifDefinitions.Apps.Hca.Datasets.efVd)
    }

@OptIn(ExperimentalUnsignedTypes::class)
val Egk.mfData: MFData get() {
    return MFData(gdo, cardVersion, cvcAuth, cvcCA, atr)
}

@OptIn(ExperimentalUnsignedTypes::class)
val Egk.iccsn: String get() {
    return gdo.toTlvSimple().tlv.let {
        if (it.tag.tagNumWithClass == 0x5f5A.toULong()) {
            it.contentAsBytesBer.toHexString()
        } else {
            null
        }
    } ?: readError("ICCSN")
}

private fun readError(toBeRead: String): Nothing =
    throw CardInsufficient(
        "Could not read $toBeRead.",
    )
