package com.epotheke.cardlink.core

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.cif.bundled.EgkCifDefinitions
import org.openecard.sal.iface.dids.PaceDid
import org.openecard.sal.sc.SmartcardApplication
import org.openecard.sal.sc.SmartcardDeviceConnection
import org.openecard.sc.apdu.checkOk
import org.openecard.sc.apdu.command.InternalAuthenticate
import org.openecard.sc.apdu.command.Mse
import org.openecard.sc.apdu.toCommandApdu
import org.openecard.sc.iface.SecureMessagingException
import org.openecard.sc.tlv.Tag
import org.openecard.sc.tlv.TlvPrimitive
import org.openecard.utils.serialization.toPrintable
import kotlin.ExperimentalUnsignedTypes
import kotlin.OptIn

private val logger = KotlinLogging.logger { }

class Egk(
    private val connection: SmartcardDeviceConnection,
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    val personalDataBytes: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.Hca.name, EgkCifDefinitions.Apps.Hca.Datasets.efPd)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val insurerDataBytes: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.Hca.name, EgkCifDefinitions.Apps.Hca.Datasets.efVd)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val gdo: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.Mf.name, EgkCifDefinitions.Apps.Mf.Datasets.efGdo)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val cardVersion: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.Mf.name, EgkCifDefinitions.Apps.Mf.Datasets.efVersion2)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val cvcAuth: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.Mf.name, EgkCifDefinitions.Apps.Mf.Datasets.ef_c_eGK_aut_cvc_e256)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val cvcCA: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.Mf.name, EgkCifDefinitions.Apps.Mf.Datasets.ef_c_ca_cs_e256)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val atr: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.Mf.name, EgkCifDefinitions.Apps.Mf.Datasets.efAtr)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val cert: UByteArray by lazy {
        readDataSetFromApp(EgkCifDefinitions.Apps.ESign.name, EgkCifDefinitions.Apps.ESign.Datasets.ef_c_ch_aut_e256)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun internalAuthenticateAutCvcE256(challenge: ByteArray): UByteArray =
        connection.applications
            .find { app -> app.name == EgkCifDefinitions.Apps.Mf.name }
            ?.let { app ->
                if (!app.isConnected) {
                    app.connect()
                }

                val mse =
                    Mse.mseSet(
                        Mse.P1Flags(
                            smInCommand = false,
                            smInResponse = false,
                            computationDecipherIntAuthKeyAgree = true,
                            verifyEncipherExtAuthKeyAgree = false,
                        ),
                        Mse.Tag.CRT_EXTERNAL_AUTHENTICATE,
                        listOf(
                            TlvPrimitive(
                                Tag.forTagNumWithClass(0x84u),
                                // keyId
                                "09".hexToUByteArray().toPrintable(),
                            ),
                            TlvPrimitive(
                                Tag.forTagNumWithClass(0x80u),
                                // algId
                                "00".hexToUByteArray().toPrintable(),
                            ),
                        ),
                    )
                val mseRes = app.device.channel.transmit(mse.apdu)
                mseRes.checkOk()

                val intAuth = InternalAuthenticate.withData(challenge.toUByteArray(), algorithm = 0x00u)
                val intAuthRes = app.device.channel.transmit(intAuth.apdu)
                intAuthRes.checkOk()
                intAuthRes.data
            } ?: throw CardInsufficient("MF application is not available")

    @OptIn(ExperimentalUnsignedTypes::class)
    internal fun authenticatePace(can: String) {
        connection.applications
            .find { app -> app.name == EgkCifDefinitions.Apps.Mf.name }
            ?.dids
            ?.find { it.name == EgkCifDefinitions.Apps.Mf.Dids.autPace }
            ?.let {
                check(it is PaceDid)
                it.application.takeUnless { app -> app.isConnected }?.connect()
                it.establishChannel(can, null, null)
            } ?: throw IllegalStateException("PaceDid not found in Cif")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun SmartcardApplication.readDataSet(name: String): UByteArray =
        datasets.find { it.name == name }?.run {
            this@readDataSet.connect()
            read()
        } ?: readError(name)

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun readDataSetFromApp(
        appName: String,
        datasetName: String,
    ): UByteArray =
        connection.applications.find { it.name == appName }?.run {
            readDataSet(datasetName)
        } ?: readError(datasetName)

    private fun readError(toBeRead: String): Nothing =
        throw CardInsufficient(
            "Could not read $toBeRead.",
        )

    @OptIn(ExperimentalUnsignedTypes::class)
    fun sendApduToCard(apdu: ByteArray): ByteArray {
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

    companion object {
        const val CAN_LEN = 6
    }
}
