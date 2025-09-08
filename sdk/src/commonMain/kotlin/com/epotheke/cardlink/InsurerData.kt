package com.epotheke.cardlink

import com.fleeksoft.charset.decodeToString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.charsets.forName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalUnsignedTypes::class)
internal fun UByteArray.toInsurerData(): AllgemeineVersicherungsdaten? =
    try {
        val start = this[0].toInt().shl(8).or(this[1].toInt())
        val len = this[2].toInt().shl(8).or(this[3].toInt())

        val vd = this.sliceArray(IntRange(start, start + len - 1))

        val xmlString =
            gunzip(
                vd,
            ).toByteArray().decodeToString(Charsets.forName("ISO-8859-15"))
        xml.decodeFromString(AllgemeineVersicherungsdaten.serializer(), xmlString)
    } catch (e: Exception) {
        logger.warn(e) { "Exception during reading of ef.vd" }
        null
    }

private const val VSDM_NAMESPACE = "http://ws.gematik.de/fa/vsdm/vsd/v5.2"

@Serializable
@XmlSerialName("UC_AllgemeineVersicherungsdatenXML", VSDM_NAMESPACE)
class AllgemeineVersicherungsdaten(
    @XmlSerialName("CDM_VERSION")
    var cdmVersion: String? = null,
    @XmlSerialName("Versicherter")
    var versicherter: VersicherterVD? = null,
)

@Serializable
@XmlSerialName("Versicherter")
class VersicherterVD(
    @XmlSerialName("Versicherungsschutz")
    var versicherungsschutz: Versicherungsschutz? = null,
    @XmlSerialName("Zusatzinfos")
    var zusatzinfos: Zusatzinfos? = null,
)

@Serializable
@XmlSerialName("Versicherungsschutz")
class Versicherungsschutz(
    @XmlSerialName("Beginn")
    @XmlElement
    var beginn: String? = null,
    @XmlSerialName("Kostentraeger")
    var kostentraeger: Kostentraeger? = null,
)

@Serializable
@XmlSerialName("Kostentraeger")
class Kostentraeger(
    @XmlSerialName("Kostentraegerkennung")
    @XmlElement
    var kostentraegerkennung: String? = null, // IK‑Nummer
    @XmlSerialName("Kostentraegerlaendercode")
    @XmlElement
    var kostentraegerlaendercode: String? = null,
    @XmlElement
    @XmlSerialName("Name")
    var name: String? = null,
)

@Serializable
class Zusatzinfos(
    @XmlSerialName("ZusatzinfosGKV")
    var zusatzinfosGKV: ZusatzinfosGKV? = null,
)

@Serializable
class ZusatzinfosGKV(
    @XmlSerialName("Versichertenart")
    @XmlElement
    var versichertenart: String? = null,
    @XmlSerialName("Zusatzinfos_Abrechnung_GKV")
    var zusatzinfosAbrechnungGKV: ZusatzinfosAbrechnungGKV? = null,
)

@Serializable
class ZusatzinfosAbrechnungGKV(
    @XmlSerialName("WOP")
    @XmlElement
    var wop: String? = null,
)
