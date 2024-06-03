package com.epotheke.cardlink.mock.encoding

import com.epotheke.cardlink.mock.GematikEnvelope
import com.epotheke.cardlink.mock.cardLinkJsonFormatter
import jakarta.websocket.Encoder
import kotlinx.serialization.encodeToString


class JsonEncoder : Encoder.Text<GematikEnvelope> {

    override fun encode(data: GematikEnvelope): String {
        return cardLinkJsonFormatter.encodeToString(data)
    }
}
