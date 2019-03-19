package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        port : Int = 8080,
        jwkSetUrl : String? = wireMockServer?.getJwksUrl(),
        tokenUrl : String? = wireMockServer?.getTokenUrl(),
        issuer : String? = wireMockServer?.baseUrl(),
        authorizedSystems : String? = wireMockServer?.getSubject(),
        aktoerRegisterBaseUrl : String? = wireMockServer?.getAktoerRegisterBaseUrl(),
        opprettSakUrl : String? = wireMockServer?.getOpprettSakUrl(),
        opprettOppgaveUrl : String? = wireMockServer?.getOpprettOppgaveUrl(),
        opprettJournalPostUrl : String? = wireMockServer?.getOpprettJournalPostUrl(),
        pleiepeingerDokumentBaseUrl : String? = wireMockServer?.getPleiepengerDokumentBaseUrl(),
        clientSecret : String? = "foo"
    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.authorization.token_url","$tokenUrl"),
            Pair("nav.authorization.jwks_url","$jwkSetUrl"),
            Pair("nav.authorization.issuer","$issuer"),
            Pair("nav.rest_api.authorized_systems","$authorizedSystems"),
            Pair("nav.aktoer_register_base_url","$aktoerRegisterBaseUrl"),
            Pair("nav.opprett_sak_url","$opprettSakUrl"),
            Pair("nav.opprett_oppgave_url","$opprettOppgaveUrl"),
            Pair("nav.opprett_journal_post_url","$opprettJournalPostUrl"),
            Pair("nav.pleiepenger_dokument_base_url","$pleiepeingerDokumentBaseUrl")
        )

        if (clientSecret != null) {
            map["nav.authorization.service_account.client_secret"] = clientSecret
        }
        return map.toMap()
    }

    fun asArray(map : Map<String, String>) : Array<String>  {
        val list = mutableListOf<String>()
        map.forEach { configKey, configValue ->
            list.add("-P:$configKey=$configValue")
        }
        return list.toTypedArray()
    }
}