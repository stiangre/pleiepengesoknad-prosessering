package no.nav.helse.joark

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.HttpError
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.dusseldorf.ktor.client.*
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.ZonedDateTime

class JoarkGateway(
    baseUrl : URI,
    private val accessTokenClient : CachedAccessTokenClient
) {

    private companion object {
        private const val JOURNALFORING_OPERATION = "journalforing"
        private val logger: Logger = LoggerFactory.getLogger("nav.JoarkGateway")
    }

    private val completeUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1","journalforing")
    ).toString()

    private val objectMapper = configuredObjectMapper()

    suspend fun journalfoer(
        aktoerId: AktoerId,
        mottatt: ZonedDateTime,
        dokumenter: List<List<URI>>,
        correlationId: CorrelationId
    ) : JournalPostId {

        val authorizationHeader = accessTokenClient.getAccessToken(setOf("openid")).asAuthoriationHeader()

        val joarkRequest = JoarkRequest(
            aktoerId = aktoerId.id,
            mottatt = mottatt,
            dokumenter = dokumenter
        )

        val body = objectMapper.writeValueAsBytes(joarkRequest)
        val contentStream = { ByteArrayInputStream(body) }

        val httpRequest = completeUrl
            .httpPost()
            .body(contentStream)
            .header(
                HttpHeaders.XCorrelationId to correlationId.value,
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.ContentType to "application/json",
                HttpHeaders.Accept to "application/json"
            )

        val (request, response, result) = Operation.monitored(
            app = "pleiepengesoknad-prosessering",
            operation = JOURNALFORING_OPERATION,
            resultResolver = { 201 == it.second.statusCode }
        ) { httpRequest.awaitStringResponseResult() }

        return result.fold(
            { success -> objectMapper.readValue(success)},
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error(error.toString())
                throw HttpError(response.statusCode, "Feil ved jorunalføring.")
            }
        )
    }

    private fun configuredObjectMapper() : ObjectMapper {
        val objectMapper = jacksonObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        objectMapper.registerModule(JavaTimeModule())
        return objectMapper
    }
}
private data class JoarkRequest(
    val aktoerId: String,
    val mottatt: ZonedDateTime,
    val dokumenter: List<List<URI>>
)

data class JournalPostId(val journalPostId: String)