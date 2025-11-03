package com.epotheke.cardlink

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.cif.bundled.EgkCifDefinitions
import org.openecard.sal.iface.dids.PaceDid
import org.openecard.sal.sc.SmartcardApplication
import org.openecard.sal.sc.SmartcardDeviceConnection
import org.openecard.sc.apdu.toCommandApdu
import org.openecard.sc.iface.SecureMessagingException
import org.openecard.sc.tlv.toTlvSimple
import kotlin.ExperimentalUnsignedTypes
import kotlin.OptIn

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalUnsignedTypes::class)
internal class MFData(
    val gdo: UByteArray,
    val cardVersion: UByteArray,
    val cvcAuth: UByteArray,
    val cvcCA: UByteArray,
    val atr: UByteArray,
)

internal class Egk(
    private val connection: SmartcardDeviceConnection,
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    val personalData: PersoenlicheVersichertendaten by lazy {
        connection.applications.find { it.name == EgkCifDefinitions.Apps.Hca.name }?.run {
            readDataSet(EgkCifDefinitions.Apps.Hca.Datasets.efPd).toPersonalData()
        } ?: readError(EgkCifDefinitions.Apps.Hca.Datasets.efPd)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val insurerData: AllgemeineVersicherungsdaten by lazy {
        connection.applications.find { it.name == EgkCifDefinitions.Apps.Hca.name }?.run {
            readDataSet(EgkCifDefinitions.Apps.Hca.Datasets.efVd).toInsurerData()
        } ?: readError(EgkCifDefinitions.Apps.Hca.Datasets.efVd)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    internal val mfData: MFData by lazy {
        connection.applications.find { it.name == EgkCifDefinitions.Apps.Mf.name }?.run {
            MFData(
                gdo = readDataSet(EgkCifDefinitions.Apps.Mf.Datasets.efGdo),
                cardVersion = readDataSet(EgkCifDefinitions.Apps.Mf.Datasets.efVersion2),
                cvcAuth = readDataSet(EgkCifDefinitions.Apps.Mf.Datasets.ef_c_eGK_aut_cvc_e256),
                cvcCA = readDataSet(EgkCifDefinitions.Apps.Mf.Datasets.ef_c_ca_cs_e256),
                atr = readDataSet(EgkCifDefinitions.Apps.Mf.Datasets.efAtr),
            )
        } ?: readError(EgkCifDefinitions.Apps.Mf.name)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val iccsn: String by lazy {
        mfData.gdo.toTlvSimple().tlv.let {
            if (it.tag.tagNumWithClass == 0x5f5A.toULong()) {
                it.contentAsBytesBer.toHexString()
            } else {
                null
            }
        } ?: readError("ICCSN")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val cert: UByteArray by lazy {
        connection.applications.find { it.name == EgkCifDefinitions.Apps.ESign.name }?.run {
            readDataSet(EgkCifDefinitions.Apps.ESign.Datasets.ef_c_ch_aut_e256)
        } ?: readError(EgkCifDefinitions.Apps.ESign.Datasets.ef_c_ch_aut_e256)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    internal fun authenticate(can: String) {
        connection.applications
            .find { app -> app.name == EgkCifDefinitions.Apps.Mf.name }
            ?.dids
            ?.find { it.name == EgkCifDefinitions.Apps.Mf.Dids.autPace }
            ?.let {
                check(it is PaceDid)
                it.establishChannel(can, null, null)
            } ?: throw IllegalStateException("PaceDid not found in Cif")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun SmartcardApplication.readDataSet(name: String): UByteArray =
        datasets.find { it.name == name }?.run {
            this@readDataSet.connect()
            read()
        } ?: readError(name)

    private fun readError(toBeRead: String): Nothing =
        throw CardInsufficient(
            "Could not read $toBeRead.",
        )

    @OptIn(ExperimentalUnsignedTypes::class)
    internal fun sendApduToCard(apdu: ByteArrayAsBase64): ByteArray {
        val cApdu = apdu.toCommandApdu()
        logger.debug { "APDU from service: $cApdu" }
        return try {
            val responseApdu = connection.channel.transmit(cApdu)
            logger.debug { "Response APDU: $responseApdu" }
            responseApdu.toBytes.toByteArray()
        } catch (e: SecureMessagingException) {
            logger.debug(e) { "Error communicating with card." }
            throw e
        }
    }
}

internal suspend fun withEgk(
    connection: SmartcardDeviceConnection,
    can: String,
    block: suspend Egk.() -> Unit,
) {
    Egk(connection).run {
        authenticate(can)
        block()
    }
}
