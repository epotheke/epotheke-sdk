package com.epotheke.cardlink.mock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.websocket.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.Base64
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}

@QuarkusTest
class CardlinkTest {

    @TestHTTPResource("/cardlink?token=123456")
    lateinit var uri: URI

    @Inject
    lateinit var objMapper: ObjectMapper

    @Test
    @Throws(Exception::class)
    fun testWebsocketChat() {
        ContainerProvider.getWebSocketContainer().connectToServer(Client::class.java, uri).use {
            Assertions.assertEquals("CONNECT", MESSAGES_TYPES.poll(10, TimeUnit.SECONDS))
            expectReadyMessage()
            expectSendApduMessage()
            sendApduResponseMessage(it)
        }
    }

    private fun expectSendApduMessage() {
        val sendApduMsg = EGK_ENVELOPE_MESSAGES.poll(10, TimeUnit.SECONDS)
        Assertions.assertEquals(EgkEnvelopeTypes.SEND_APDU_ENVELOPE, MESSAGES_TYPES.poll(10, TimeUnit.SECONDS))

        val sendApduPayload = objMapper.readValue(
            Base64.getDecoder().decode(sendApduMsg?.payload),
            SendApduPayload::class.java
        )
        Assertions.assertEquals(CARD_SESSION_ID, sendApduPayload.cardSessionId)
    }

    private fun expectReadyMessage() {
        EGK_ENVELOPE_MESSAGES.poll(10, TimeUnit.SECONDS)
        Assertions.assertEquals(EgkEnvelopeTypes.READY, MESSAGES_TYPES.poll(10, TimeUnit.SECONDS))
    }

    private fun sendApduResponseMessage(session: Session) {
        val sendApduResponseMsg = String.format(SEND_APDU_RESPONSE_MESSAGE, CARD_SESSION_ID, correlationId)
        session.asyncRemote.sendText(sendApduResponseMsg)
    }

    @ClientEndpoint
    class Client {

        @Inject
        lateinit var objMapper: ObjectMapper

        @OnOpen
        fun open(session: Session) {
            MESSAGES_TYPES.add("CONNECT")

            // Send a register eGK Message after connecting
            val registerEgkEnvelope = String.format(REGISTER_EGK_MESSAGE, CARD_SESSION_ID)
            session.asyncRemote.sendText(registerEgkEnvelope)
        }

        @OnMessage
        fun message(data: String) {
            logger.debug { "Received message at client: $data" }

            val payload = objMapper.readValue(data, JsonNode::class.java)

            if (! payload.nodeType.equals(JsonNodeType.ARRAY)) {
                throw IllegalStateException("Payload is not from type array.")
            } else {
                val egkEnvelope = objMapper.treeToValue(payload.get(0), EgkEnvelope::class.java)
                correlationId = payload.get(2)?.textValue()
                MESSAGES_TYPES.add(egkEnvelope.type)
                EGK_ENVELOPE_MESSAGES.add(egkEnvelope)
            }
        }
    }

    companion object {
        private const val CARD_SESSION_ID = "3509584a-04e7-4756-bab0-0da4a84d0c12"

        private val MESSAGES_TYPES = LinkedBlockingDeque<String>()
        private val EGK_ENVELOPE_MESSAGES = LinkedBlockingDeque<EgkEnvelope>()

        private var correlationId : String? = null

        private const val REGISTER_EGK_MESSAGE = """
            [
              {
                "type": "registerEGK",
                "payload": "eyJjYXJkU2Vzc2lvbklkIjoiMzUwOTU4NGEtMDRlNy00NzU2LWJhYjAtMGRhNGE4NGQwYzEyIiwiZ2RvIjoiV2dxQUoyaUJHWm1abG9OcCIsImF0ciI6IjRCRUNBZ2dKQWdNQWdBSUNBZ2dKQWdJSUNWOVNESUJtQlVSRlJ5dEVjNVloNE5BREJBUUIwaEJFUlVsR1dEVXlSalJIUkVwTkFRQUEweEJFUlVjclJGTXpOMDlUUjBzeUFRQUExQkJFUlVjclJGTXpOMGRMVmpBekFRQUExUU1FQkFIV0VFUkZSeXRFUTBJeVExUkhNREVCQUFEWEVFUkZSeXRFVUVSUVUxOWZYMThCQmdBPSIsImNhcmRWZXJzaW9uIjoiN3l2QUF3SUFBTUVEQkFVQndoQkVSVWNyUkZNek4wZExWakF6QVFBQXhBTUJBQURGQXdJQUFNY0RBUUFBIiwieDUwOUF1dGhSU0EiOiJNSUlFOHpDQ0E5dWdBd0lCQWdJRExKeFwvTUEwR0NTcUdTSWIzRFFFQkN3VUFNSUdzTVFzd0NRWURWUVFHRXdKRVJURXpNREVHQTFVRUNnd3FRWFJ2Y3lCSmJtWnZjbTFoZEdsdmJpQlVaV05vYm05c2IyZDVJRWR0WWtnZ1RrOVVMVlpCVEVsRU1VVXdRd1lEVlFRTEREeEZiR1ZyZEhKdmJtbHpZMmhsSUVkbGMzVnVaR2hsYVhSemEyRnlkR1V0UTBFZ1pHVnlJRlJsYkdWdFlYUnBhMmx1Wm5KaGMzUnlkV3QwZFhJeElUQWZCZ05WQkFNTUdFRlVUMU11UlVkTExVTkJNakEySUZSRlUxUXRUMDVNV1RBZUZ3MHlNekEwTWpBeE1UTTNOREJhRncweU9EQTBNakF4TVRNM05EQmFNSUdSTVFzd0NRWURWUVFHRXdKRVJURWZNQjBHQTFVRUNnd1dRVTlMSUVKaFpHVnVMVmZEdkhKMGRHVnRZbVZ5WnpFU01CQUdBMVVFQ3d3Sk1UQTRNREU0TURBM01STXdFUVlEVlFRTERBcFVNRFE1T1RBME56STNNUTh3RFFZRFZRUUVEQVpFWlhCcWNHa3hEekFOQmdOVkJDb01Ca3RoWjNWNGFURVdNQlFHQTFVRUF3d05SR1Z3YW5CcElFdGhaM1Y0YVRDQ0FTSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBSlcyc1VvSEh2QkF3a0luRE91VlRWeWQ4TERDR0FOc09EZmdXK2s5Wm9nblhaYnp6WWxZT3hCcDJcL01Bd0NGdkwwODRKMklTZFJvM2FBRE1zc0tlXC9uaEdqMTZlKzg2UE5OTklIZ3lEKzdNQzEzQlNDYzlCYXZjZTZSTnkzcFR3dGt6OU1NdjAxTTY1Z3NoSHd3N1A1YmZySGdBNzVCNG1RUTlhalhVQWxCb01CanU2YU5BZVJjZnJyaVhKTXhaVmJ1TGFBYXlWWVhJdWdSY1AzRTVpSVBYTkZoNTczTGVZRmZCMVpFTmtUXC9oUEh0KzE2aEdyUXl1M3pDOEtkN2pFYmlKUFwveE8yclVXbXQ3N3VHTU1zMnJiRmZoQUhtVlwvaERPTTRnQ0JQWDlBQVBObUlBMlRETTJKVzRXVU5kQSt4Rllscm1ZbEU0YWswWFQyc1g5WURZQnNDQXdFQUFhT0NBVFV3Z2dFeE1CMEdBMVVkRGdRV0JCU2RLTVRGb0NPRVwvUmw2S3VEc3M2VGF0eU00d0RBT0JnTlZIUThCQWY4RUJBTUNCNEF3REFZRFZSMFRBUUhcL0JBSXdBREJSQmdOVkhTQUVTakJJTUFrR0J5cUNGQUJNQkVZd093WUlLb0lVQUV3RWdTTXdMekF0QmdnckJnRUZCUWNDQVJZaGFIUjBjRG92TDNkM2R5NW5aVzFoZEdsckxtUmxMMmR2TDNCdmJHbGphV1Z6TURjR0NDc0dBUVVGQndFQkJDc3dLVEFuQmdnckJnRUZCUWN3QVlZYmFIUjBjRG92TDI5amMzQXVaV2RyTFhSbGMzUXRkSE53TG1SbE1COEdBMVVkSXdRWU1CYUFGTEFreWRhWnhKQTJMalFWMWR6S1BNN3hhdVBcL01CTUdBMVVkSlFRTU1Bb0dDQ3NHQVFVRkJ3TUNNREFHQlNza0NBTURCQ2N3SlRBak1DRXdIekFkTUJBTURsWmxjbk5wWTJobGNuUmxMeTF5TUFrR0J5cUNGQUJNQkRFd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFDNWxzbSs4bEdnOGw3ZmYwcEhObUZpVWppb2lwWUhxTDFYSXhyaWM4WVlKVjlBT0lqTG8rUVNzR0JLbm9YTTFPQ1FCWjJTc1pESHExMHczZGpUdm9DK0hHeWJxR0V1VnBJbWVRRWZwRzJaTmZMZUhnaXB4YzY3ZjF6cjFENGdzaGNKRW1iM0hiTCthM0lRV1lXNWNUdXZsbGMxQ09TOVwvRGd4R3FyOHh0S0k2K1phMWs5ZnZLblNKZVFxeDBNdnZQbitmNHBleWV6V2dCYVlwYWt3c2loZjhxZGNRdEJkdlwvSTNjSmNZekF3aTNBNE54bVptTktlMmlOVkF3Smh4ZjRNQVJzQVlVZ3M0YnQ0VzNHaFhHeXZwdUVRb0tJSm9zKzZsZEtVb2Z1cXBjRFwvOTRsVlRESCszZnl5azFnU3Z2eHdXYWwyWEZXYVRiSXkwTnJCN05jSzA9IiwieDUwOUF1dGhFQ0MiOiJNSUlEYURDQ0F3NmdBd0lCQWdJREw5QUhNQW9HQ0NxR1NNNDlCQU1DTUlHc01Rc3dDUVlEVlFRR0V3SkVSVEV6TURFR0ExVUVDZ3dxUVhSdmN5QkpibVp2Y20xaGRHbHZiaUJVWldOb2JtOXNiMmQ1SUVkdFlrZ2dUazlVTFZaQlRFbEVNVVV3UXdZRFZRUUxERHhGYkdWcmRISnZibWx6WTJobElFZGxjM1Z1WkdobGFYUnphMkZ5ZEdVdFEwRWdaR1Z5SUZSbGJHVnRZWFJwYTJsdVpuSmhjM1J5ZFd0MGRYSXhJVEFmQmdOVkJBTU1HRUZVVDFNdVJVZExMVU5CTWpBMUlGUkZVMVF0VDA1TVdUQWVGdzB5TXpBME1qQXhNVE00TUROYUZ3MHlPREEwTWpBeE1UTTRNRE5hTUlHUk1Rc3dDUVlEVlFRR0V3SkVSVEVmTUIwR0ExVUVDZ3dXUVU5TElFSmhaR1Z1TFZmRHZISjBkR1Z0WW1WeVp6RVNNQkFHQTFVRUN3d0pNVEE0TURFNE1EQTNNUk13RVFZRFZRUUxEQXBVTURRNU9UQTBOekkzTVE4d0RRWURWUVFFREFaRVpYQnFjR2t4RHpBTkJnTlZCQ29NQmt0aFozVjRhVEVXTUJRR0ExVUVBd3dOUkdWd2FuQnBJRXRoWjNWNGFUQmFNQlFHQnlxR1NNNDlBZ0VHQ1Nza0F3TUNDQUVCQndOQ0FBUkdcL3pIamVyMkpWcE5pOGZHTHcxVFk4VVYwakM1T0l5M0ViUFNLWXpFWTR3ajF3dTFVeG16MHFoQXZ1Q1RUbTlkSE9ha0w1MlB0d2lWNFwvRkIwXC9UdU9vNElCTlRDQ0FURXdIUVlEVlIwT0JCWUVGSlNrYys3TkF2aEczcjMrbnlHZmQ3RzRNZkRlTUE0R0ExVWREd0VCXC93UUVBd0lIZ0RBTUJnTlZIUk1CQWY4RUFqQUFNRkVHQTFVZElBUktNRWd3Q1FZSEtvSVVBRXdFUmpBN0JnZ3FnaFFBVEFTQkl6QXZNQzBHQ0NzR0FRVUZCd0lCRmlGb2RIUndPaTh2ZDNkM0xtZGxiV0YwYVdzdVpHVXZaMjh2Y0c5c2FXTnBaWE13TndZSUt3WUJCUVVIQVFFRUt6QXBNQ2NHQ0NzR0FRVUZCekFCaGh0b2RIUndPaTh2YjJOemNDNWxaMnN0ZEdWemRDMTBjM0F1WkdVd0h3WURWUjBqQkJnd0ZvQVVScVA1WGlkb3dZXC9uWjB4UWFpUWZHMjF0OWx3d0V3WURWUjBsQkF3d0NnWUlLd1lCQlFVSEF3SXdNQVlGS3lRSUF3TUVKekFsTUNNd0lUQWZNQjB3RUF3T1ZtVnljMmxqYUdWeWRHVXZMWEl3Q1FZSEtvSVVBRXdFTVRBS0JnZ3Foa2pPUFFRREFnTklBREJGQWlFQWhZVEtEd0xva29JbGJLUXk5dGhVSUR2XC9Dclk1aURISVNlVHk4c0E1Rll3Q0lEeHBiZ2dHM3dPN3dZXC9FMlwvWlA5YWYzalAyWXJvQ0JxUVVBVHFzbmFGZWYiLCJjdmNBdXRoIjoiZnlHQjJuOU9nWk5mS1FGd1FnaEVSVUZVV0JZQ0lYOUpTd1lHS3lRREJRTUJoa0VFSVg0a0p4Rk0zbmNHemJCVitUWmZkelVIbFNcL1wvT0FZbnJMTE50SGdHS280U2JvaFFJdWIwZUo2NDg0QmhrVDBha3A2OCszODhWWDk3bjAxR3F1KzdTbDhnREFBSmdDZG9nUm1abVphRGFYOU1Fd1lJS29JVUFFd0VnUmhUQndBQUFBQUFBQUJmSlFZQ0F3QUVBZ0JmSkFZQ0NBQUVBZ0JmTjBDSzdJZm05OTh3VmR1cXZCbTAzcWdrZG9yTVJ0a3hTXC9FOTFRZzVhNlh5XC9JY2NPRTVFcjEyQzRGTFM4QkYrN2NwajJhVjVQekptMUIwdVc3WFJLXC9YaiIsImN2Y0NBIjoiZnlHQjJIOU9nWkZmS1FGd1FnaEVSVWRZV0lZQ0lIOUpUUVlJS29aSXpqMEVBd0tHUVFSajN0TmRpXC9KMFpzeGgrU2tHUzVEdWFiXC9tRm56Y0p1WFdvQ1Y3YlhPOE42a3g1aXExWHhIbnZTcDdoSXppMEVvOTdqcEZwQmtsOGRWblVSdUFmRUszWHlBSVJFVkJWRmdXQWlGXC9UQk1HQ0NxQ0ZBQk1CSUVZVXdlQUFBQUFBQUFBWHlVR0FnRUFBZ0VIWHlRR0Fna0FBZ0VHWHpkQWU0XC9nRndUeHFnOEk5VzRxZ2pWcFwvOThiRjJEam80WlwvXC93ZGFSeDE0bFlodTJZRmRsSCtwWnBicnBoS2VSRUxVQmZqdElkRTUzWjJ3MWRJUDJHNktVdz09IiwiY2xpZW50IjoiQ09NIn0="
              },
              "%s"
            ]
        """

        private const val SEND_APDU_RESPONSE_MESSAGE = """
            [
              {
                "type": "sendAPDUResponse",
                "payload": "eyJjYXJkU2Vzc2lvbklkIjoiMzUwOTU4NGEtMDRlNy00NzU2LWJhYjAtMGRhNGE4NGQwYzEyIiwicmVzcG9uc2UiOiJrYk5BSzR4djRueXZ3RU80ZndnbVptbkZud0dTYjIrV0RmdEpxMlN1czBsd3g1clFVUTl0cTBRMXFLd2x5anhwYUFMMENTSjFnV1JWUWtmVnpvWXh4WkFBIn0="
              },
              "%s",
              "%s"
            ]
        """
    }
}
