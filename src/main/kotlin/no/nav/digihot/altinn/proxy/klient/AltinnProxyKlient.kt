package no.nav.digihot.altinn.proxy.klient

import com.github.kittinunf.fuel.core.Headers.Companion.ACCEPT
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.isClientError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result
import no.nav.digihot.altinn.proxy.klient.error.ProxyError
import no.nav.digihot.altinn.proxy.klient.error.ProxyErrorMedResponseBody
import no.nav.digihot.altinn.proxy.klient.error.exceptions.AltinnException
import no.nav.digihot.altinn.proxy.klient.error.exceptions.AltinnProxyException
import no.nav.digihot.altinn.proxy.klient.error.exceptions.AltinnProxyKlientException
import no.nav.digihot.altinn.proxy.klient.model.AltinnReportee
import no.nav.digihot.altinn.proxy.klient.model.SelvbetjeningToken
import no.nav.digihot.altinn.proxy.klient.model.ServiceCode
import no.nav.digihot.altinn.proxy.klient.model.ServiceEdition
import no.nav.digihot.altinn.proxy.klient.utils.CorrelationIdUtils
import no.nav.digihot.altinn.proxy.klient.utils.ResourceUtils
import org.slf4j.LoggerFactory

class AltinnProxyKlient(
        private val consumerId: String,
        private val proxyUrl: String
) {

    private var klientVersjon: String = ResourceUtils.getKlientVersjon()

    // An earlier version had a fallback functionality:
    //      If the Altinn proxy failed, it would call Altinn directly.
    //      This has been removed to reduce complexity

    /**
     * Hent alle organisasjoner i Altinn en bruker har enkel rettighet i
     *  @param selvbetjeningToken - Selvbetjening token til innlogget bruker
     *  @param serviceCode - Kode for rettigheter brukeren har for en organisasjon (henger sammen med ServiceEdition)
     *  @param serviceEdition
     *  @param filtrerPaaAktiveOrganisasjoner - Aktiver filtering på både Status og Type
     *
     *  @return en liste av alle organisasjoner
     *   - med Status: 'Active' og Type: 'Enterprise' | 'Business', når filtrerPaaAktiveOrganisasjoner er 'true'
     *   - med Status: 'Active' | 'Inactive' og Type: 'Enterprise' | 'Business' | 'Person', når filtrerPaaAktiveOrganisasjoner er 'false'
     */
    fun hentOrganisasjoner(
            selvbetjeningToken: SelvbetjeningToken,
            serviceCode: ServiceCode,
            serviceEdition: ServiceEdition,
            filtrerPaaAktiveOrganisasjoner: Boolean
    ): List<AltinnReportee> {
        val organisasjoner: ArrayList<AltinnReportee> = ArrayList()
        var detFinnesFlereOrganisasjoner = true

        val filterValue = if (filtrerPaaAktiveOrganisasjoner) QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER else null

        while (detFinnesFlereOrganisasjoner) {
            try {
                val nyeOrganisasjoner = hentOrganisasjonerViaAltinnProxy(
                        selvbetjeningToken, serviceCode, serviceEdition, DEFAULT_PAGE_SIZE, organisasjoner.size, filterValue
                )

                if (nyeOrganisasjoner.size > DEFAULT_PAGE_SIZE) {
                    logger.error("Altinn returnerer flere organisasjoner (${nyeOrganisasjoner.size}) " +
                            "enn det vi spurte om ($DEFAULT_PAGE_SIZE). " +
                            "Dette medfører at brukeren ikke får tilgang til alle bedriftene sine")
                }

                if (nyeOrganisasjoner.size != DEFAULT_PAGE_SIZE) {
                    detFinnesFlereOrganisasjoner = false
                }

                organisasjoner.addAll(nyeOrganisasjoner)
            } catch (proxyException: AltinnProxyException) {
                logger.warn("Fikk en feil i digihot-altinn-proxy med melding '${proxyException.message}'.")
                throw proxyException
            } catch (altinnException: AltinnException) {
                logger.warn("Fikk exception i Altinn med følgende melding '${altinnException.message}'. " +
                        "Exception fra Altinn håndteres av klientapplikasjon")
                throw altinnException
            } catch (exception: Exception) {
                logger.warn("Fikk exception med følgende melding '${exception.message}'. " +
                        "Denne skal håndteres av klientapplikasjon")
                throw AltinnProxyKlientException("Exception ved kall til proxy", exception)
            }
        }

        return organisasjoner
    }

    private fun hentOrganisasjonerViaAltinnProxy(
            selvbetjeningToken: SelvbetjeningToken,
            serviceCode: ServiceCode?,
            serviceEdition: ServiceEdition?,
            top: Number,
            skip: Number,
            filter: String?
    ): List<AltinnReportee> {

        val parametreTilProxy = mutableMapOf<String, String>()

        if (serviceCode != null) parametreTilProxy["serviceCode"] = serviceCode.value
        if (serviceEdition != null) parametreTilProxy["serviceEdition"] = serviceEdition.value
        if (filter != null) parametreTilProxy["filter"] = filter

        parametreTilProxy["top"] = top.toString()
        parametreTilProxy["skip"] = skip.toString()

        val (_, response, result) = with(
                getAltinnProxyURL(proxyUrl, PROXY_ENDEPUNKT_API_ORGANISASJONER)
                        .httpGet(parametreTilProxy.toList())
        ) {
            authentication().bearer(selvbetjeningToken.value)
            headers[PROXY_KLIENT_VERSJON_HEADER_NAME] = klientVersjon
            headers[CORRELATION_ID_HEADER_NAME] = CorrelationIdUtils.getCorrelationId()
            headers[CONSUMER_ID_HEADER_NAME] = consumerId
            headers[ACCEPT] = "application/json"

            responseObject<List<AltinnReportee>>()
        }
        when (result) {
            is Result.Failure -> {
                val proxyErrorMedResponseBody = ProxyErrorMedResponseBody.parse(
                        response.body().toStream(),
                        response.statusCode
                )

                logger.info("Mottok en feil fra kilde '${proxyErrorMedResponseBody.kilde}' " +
                        "med status '${response.statusCode}' " +
                        "og melding '${response.responseMessage}'")

                if ((response.isClientError && response.statusCode != 404) || proxyErrorMedResponseBody.kilde == ProxyError.Kilde.ALTINN) {
                    throw AltinnException(proxyErrorMedResponseBody)
                } else {
                    throw AltinnProxyException(proxyErrorMedResponseBody)
                }


            }
            is Result.Success -> return result.get()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        const val DEFAULT_PAGE_SIZE = 500
        const val QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER = "Type ne 'Person' and Status eq 'Active'"

        const val PROXY_KLIENT_VERSJON_HEADER_NAME = "X-Proxyklient-Versjon"
        const val CORRELATION_ID_HEADER_NAME = "X-Correlation-ID"
        const val CONSUMER_ID_HEADER_NAME = "X-Consumer-ID"

        const val PROXY_ENDEPUNKT_API_ORGANISASJONER = "/v2/organisasjoner"

        fun getAltinnProxyURL(basePath: String, endepunkt: String) =
                basePath.removeSuffix("/") + endepunkt
    }
}

