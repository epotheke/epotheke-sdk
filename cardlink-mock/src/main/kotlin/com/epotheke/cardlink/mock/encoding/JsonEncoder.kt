package com.epotheke.cardlink.mock.encoding

import com.fasterxml.jackson.databind.JsonNode
import jakarta.websocket.Encoder


class JsonEncoder : Encoder.Text<JsonNode> {

    override fun encode(data: JsonNode): String {
        return data.toPrettyString()
    }
}
