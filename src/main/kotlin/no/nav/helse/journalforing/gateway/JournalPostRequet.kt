package no.nav.helse.journalforing.gateway

internal data class JournalPostRequest(
    val forsokEndeligJF: Boolean,
    val forsendelseInformasjon: ForsendelseInformasjon,
    val dokumentInfoHoveddokument: Dokument,
    val dokumentInfoVedlegg : List<Dokument>
)


internal data class ForsendelseInformasjon(
    val bruker: Map<String, Map<String, Map<String, String>>>,
    val tema: String, /// OMS
    val kanalReferanseId: String, // Vår unike ID
    val forsendelseMottatt: String, // yyyy-MM-dd'T'HH:mm:ssZ
    val forsendelseInnsendt: String, // yyyy-MM-dd'T'HH:mm:ssZ
    val mottaksKanal: String // NAV_NO
)

internal data class Dokument(
    val tittel: String,
    val dokumentVariant: List<DokumentVariant>
)

internal data class DokumentVariant(
    val arkivFilType: ArkivFilType,
    val variantFormat: VariantFormat,
    val dokument: ByteArray
)

enum class ArkivFilType  {
    PDFA,
    XML,
    JSON
}

enum class VariantFormat  {
    ORIGINAL,
    ARKIV
}