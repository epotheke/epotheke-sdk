package com.epotheke.sdk

import com.fleeksoft.charset.Charsets
import com.fleeksoft.charset.decodeToString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.hex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

private val logger = KotlinLogging.logger {  }

fun decodeHexPersonalDataXml(hexString: String): PersoenlicheVersichertendaten? = try {

        val xmlString = hex(hexString).decodeToString(
            Charsets.forName("ISO-8859-15")
        )

        xml.decodeFromString(
            PersoenlicheVersichertendaten.serializer(),
            xmlString
        )

    } catch (e: Exception) {
        logger.error(e) { "Error parsing personal data from eGK. Returning null." }
        null
    }

fun PersoenlicheVersichertendaten.json() = Json.encodeToString(this)

const val vsdmNamespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2"

@Serializable
@XmlSerialName("UC_PersoenlicheVersichertendatenXML", vsdmNamespace)
class PersoenlicheVersichertendaten {

    @XmlSerialName("CDM_VERSION")
    var comVersion: String? = null

    @XmlSerialName("Versicherter")
    var versicherter: Versicherter? = null
}

@Serializable
@XmlSerialName("Versicherter")
class Versicherter {

    @XmlSerialName("Versicherten_ID")
    @XmlElement
    lateinit var versichertenId: String

    @XmlSerialName("Person")
    var person: Person? = null
}

@Serializable
@XmlSerialName("Person")
class Person {

    @XmlSerialName("Geburtsdatum")
    @XmlElement
    lateinit var geburtsdatum: String

    @XmlSerialName("Vorname")
    @XmlElement
    lateinit var vorname: String

    @XmlSerialName("Nachname")
    @XmlElement
    lateinit var nachname: String

    @XmlSerialName("Geschlecht")
    @XmlElement
    lateinit var geschlecht: String

    @XmlSerialName("Vorsatzwort")
    @XmlElement
    var vorsatzwort: String? = null

    @XmlSerialName("Namenszusatz")
    @XmlElement
    var namenszusatz: String? = null

    @XmlSerialName("Titel")
    @XmlElement
    var titel: String? = null

    @XmlSerialName("PostfachAdresse")
    var postfachAdresse: PostfachAdresse? = null


    @XmlSerialName("StrassenAdresse")
    var strassenAdresse: StrassenAdresse? = null
}


@Serializable
class PostfachAdresse {

    @XmlSerialName("Postleitzahl")
    var postleitzahl: String? = null

    @XmlSerialName("Ort")
    @XmlElement
    lateinit var ort: String

    @XmlSerialName("Postfach")
    @XmlElement
    lateinit var postfach: String

    @XmlSerialName("Land")
    lateinit var land: Land
}

@Serializable
class StrassenAdresse {

    @XmlSerialName("Postleitzahl")
    @XmlElement
    var postleitzahl: String? = null

    @XmlSerialName("Ort")
    @XmlElement
    lateinit var ort: String

    @XmlSerialName("Land")
    lateinit var land: Land

    @XmlSerialName("Strasse")
    @XmlElement
    var strasse: String? = null

    @XmlSerialName("Hausnummer")
    @XmlElement
    var hausnummer: String? = null

    @XmlSerialName("Anschriftenzusatz")
    @XmlElement
    var anschriftenzusatz: String? = null
}

@Serializable
class Land {

    @XmlSerialName("Wohnsitzlaendercode")
    @XmlElement
    lateinit var wohnsitzlaendercode: String
}
@OptIn(ExperimentalXmlUtilApi::class)
private val xml = XML {
    isUnchecked = true
    repairNamespaces = true
    policy = DefaultXmlSerializationPolicy.Builder()
        .apply {
            ignoreNamespaces()
            ignoreUnknownChildren()
            verifyElementOrder
        }
        .build()
}
