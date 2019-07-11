package no.nav.helse.prosessering.v1

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktoerId
import no.nav.helse.aktoer.AktoerService
import no.nav.helse.aktoer.Fodselsnummer
import no.nav.helse.dokument.DokumentService
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.SoknadId
import org.slf4j.LoggerFactory

internal class PreprosseseringV1Service(
    private val aktoerService: AktoerService,
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseser(
        melding: MeldingV1,
        metadata: Metadata
    ) : PreprossesertMeldingV1 {
        val soknadId = SoknadId(melding.soknadId)
        logger.info("Preprosseserer $soknadId")

        val correlationId = CorrelationId(metadata.correlationId)

        val sokerAktoerId = AktoerId(melding.soker.aktoerId)

        logger.info("Søkerens AktørID = $sokerAktoerId")

        logger.trace("Henter AktørID for barnet.")
        val barnAktoerId = hentBarnetsAktoerId(barn = melding.barn, correlationId = correlationId)

        logger.info("Barnets AktørID = $barnAktoerId")

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")

        val soknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.trace("Generering av Oppsummerings-PDF OK.")
        logger.trace("Mellomlagrer Oppsummerings-PDF.")

        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = soknadOppsummeringPdf,
            correlationId = correlationId,
            aktoerId = sokerAktoerId
        )

        logger.trace("Mellomlagring av Oppsummerings-PDF OK")

        logger.trace("Mellomlagrer Oppsummerings-JSON")

        val soknadJsonUrl = dokumentService.lagreSoknadsMelding(
            melding = melding,
            aktoerId = sokerAktoerId,
            correlationId = correlationId
        )

        logger.trace("Mellomlagrer Oppsummerings-JSON OK.")


        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                soknadJsonUrl
            )
        )

        if (melding.vedleggUrls.isNotEmpty()) {
            logger.trace("Legger til ${melding.vedleggUrls.size} vedlegg URL's fra meldingen som dokument.")
            melding.vedleggUrls.forEach { komplettDokumentUrls.add(listOf(it))}
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        melding.reportMetrics()

        return PreprossesertMeldingV1(
            dokumentUrls = komplettDokumentUrls.toList(),
            melding = melding,
            sokerAktoerId = sokerAktoerId,
            barnAktoerId = barnAktoerId
        )
    }

    private suspend fun hentBarnetsAktoerId(
        barn: Barn,
        correlationId: CorrelationId
    ): AktoerId? {
        return if (barn.fodselsnummer != null) {
            try {
                aktoerService.getAktorId(
                    fnr = Fodselsnummer(barn.fodselsnummer),
                    correlationId = correlationId
                )
            } catch (cause: Throwable) {
                logger.warn("Feil ved oppslag på Aktør ID basert på barnets fødselsnummer. Kan være at det ikke er registrert i Aktørregisteret enda. ${cause.message}")
                null
            }
        } else null
    }
}