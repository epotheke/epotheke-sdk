package com.epotheke.cardlink.mock

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val logger = KotlinLogging.logger {}

typealias ByteArrayAsBase64 =
    @Serializable(ByteArrayAsBase64Serializer::class)
    ByteArray

typealias ByteArrayOfHex = ByteArray

@OptIn(ExperimentalEncodingApi::class)
object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor = PrimitiveSerialDescriptor("ByteArrayAsBase64Serializer", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ByteArray,
    ) {
        val base64Encoded =
            Base64
                .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                .encode(value)
        encoder.encodeString(base64Encoded)
    }

    override fun deserialize(decoder: Decoder): ByteArray =
        Base64
            .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(decoder.decodeString())
}

@OptIn(ExperimentalEncodingApi::class)
object ByteArrayOfHexAsBase64Serializer : KSerializer<ByteArrayOfHex> {
    override val descriptor = PrimitiveSerialDescriptor("ByteArrayOfHexAsBase64Serializer", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ByteArray,
    ) {
        val base64Encoded =
            Base64
                .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                .encode(value)
        encoder.encodeString(base64Encoded)
    }

    override fun deserialize(decoder: Decoder): ByteArray =
        Base64
            .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(decoder.decodeString())
}

object ByteArrayOfHexAsHexSerializer : KSerializer<ByteArrayOfHex> {
    override val descriptor = PrimitiveSerialDescriptor("ByteArrayOfHexAsHexSerializer", PrimitiveKind.STRING)

    @OptIn(ExperimentalStdlibApi::class)
    override fun serialize(
        encoder: Encoder,
        value: ByteArray,
    ) {
        encoder.encodeString(value.toHexString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun deserialize(decoder: Decoder): ByteArray = decoder.decodeString().hexToByteArray()
}

typealias ZonedDateTimeSerde =
    @Serializable(ZonedDateTimeSerializer::class)
    ZonedDateTime

/**
 * Serializer for ZonedDateTime that can handle the optional time and zone value and defaults local dates to Europe/Berlin.
 */
object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("ZonedDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ZonedDateTime,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        val value = decoder.decodeString()
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: DateTimeParseException) {
            // the time and zone value is optional, try without it
            val date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.of("Europe/Berlin"))
        }
    }
}

typealias LocalDateSerde =
    @Serializable(LocalDateSerializer::class)
    LocalDate

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateSerializer", PrimitiveKind.STRING)

    private val dateRegex = """(\d{4})(?:-(\d{2}))?(?:-(\d{2}))?""".toRegex()

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        val originalValue = decoder.decodeString()

        val match =
            dateRegex.matchEntire(originalValue)
                ?: throw DateTimeParseException("Input did not match date regex", originalValue, 0)
        val (year, month, day) = match.destructured
        return LocalDate.of(year.toInt(), month.ifBlank { "1" }.toInt(), day.ifBlank { "1" }.toInt())
    }
}

typealias InstantSerde =
    @Serializable(InstantSerializer::class)
    Instant

object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("InstantSerializer", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

typealias DurationSerde =
    @Serializable(DurationSerializer::class)
    Duration

object DurationSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("DurationSerializer", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Duration,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Duration = Duration.parse(decoder.decodeString())
}

typealias UUIDSerde =
    @Serializable(UUIDStringSerializer::class)
    UUID

object UUIDStringSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUIDStringSerializer", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: UUID,
    ) {
        val string = value.toString()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): UUID {
        val string = decoder.decodeString()
        return UUID.fromString(string)
    }
}
