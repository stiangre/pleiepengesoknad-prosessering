package no.nav.helse.gosys

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.*
import no.nav.helse.CorrelationId
import no.nav.helse.HttpRequest
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.systembruker.SystembrukerService
import java.net.URL
import java.time.ZonedDateTime

class JoarkGateway(
    private val httpClient : HttpClient,
    private val url : URL,
    private val systembrukerService: SystembrukerService
) {
    suspend fun journalfoer(
        aktoerId: AktoerId,
        mottatt: ZonedDateTime,
        dokumenter: List<List<URL>>,
        correlationId: CorrelationId
    ) : JournalPostId {

        val request = JoarkRequest(
            aktoerId = aktoerId.id,
            mottatt = mottatt,
            dokumenter = dokumenter
        )

        val httpRequest = HttpRequestBuilder()
        httpRequest.header(HttpHeaders.XCorrelationId, correlationId.value)
        httpRequest.header(HttpHeaders.Authorization, systembrukerService.getAuthorizationHeader())
        httpRequest.method = HttpMethod.Post
        httpRequest.contentType(ContentType.Application.Json)
        httpRequest.body = request
        httpRequest.url(url)

        return HttpRequest.monitored(
            httpClient = httpClient,
            expectedStatusCodes = listOf(HttpStatusCode.Created),
            httpRequest = httpRequest
        )
    }
}
private data class JoarkRequest(
    val aktoerId: String,
    val mottatt: ZonedDateTime,
    val dokumenter: List<List<URL>>
)

data class JournalPostId(val journalPostId: String)