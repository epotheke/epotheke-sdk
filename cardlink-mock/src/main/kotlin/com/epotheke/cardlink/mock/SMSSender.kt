package com.epotheke.cardlink.mock

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient


interface SMSSender {

    fun createMessage(msg: SMSCreateMessage)

}

data class SMSCreateMessage(
    var smsCode: String,
    var recipient: String,
)

@ApplicationScoped
class SpryngsmsSender : SMSSender {

    @RestClient
    lateinit var spryngsmsClient: SpryngsmsClient

    @ConfigProperty(name = "spryngsms.api-key")
    lateinit var apiKey: String

    override fun createMessage(msg: SMSCreateMessage) {
        val createMessage = SpryngsmsCreateMessage(
            body = "Your SMS-Code for Cardlink: ${msg.smsCode}",
            originator = "Cardlink",
            recipients = listOf(msg.recipient)
        )

        spryngsmsClient.createMessage("Bearer $apiKey", createMessage)
    }
}


@RegisterRestClient(configKey = "spryngsms")
@Path("v1")
interface SpryngsmsClient {

    @POST
    @Path("messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createMessage(
        @HeaderParam("Authorization") apiKey: String,
        createMessage: SpryngsmsCreateMessage,
    ) : SpryngsmsCreateMessageResponse

}

data class SpryngsmsCreateMessage(
    val encoding: String = "auto",
    val body: String,
    val route: String = "business",
    val originator: String,
    val recipients: List<String>,
)

data class SpryngsmsCreateMessageResponse(
    val id: String,
    val encoding: String,
    val originator: String,
    val body: String,
    val reference: String?,
    val credits: Double,
    @JsonProperty("scheduled_at")
    val scheduledAt: String,
    @JsonProperty("updated_at")
    val updatedAt: String,
    val links: SpryngsmsLinks,
)

data class SpryngsmsLinks(
    val self: String
)
