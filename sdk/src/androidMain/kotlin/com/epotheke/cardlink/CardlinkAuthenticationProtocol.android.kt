package com.epotheke.cardlink

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

@OptIn(markerClass = [ExperimentalUnsignedTypes::class])
actual fun gunzip(data: UByteArray): UByteArray =
        GZIPInputStream(ByteArrayInputStream(data.toByteArray())).readBytes().toUByteArray()


