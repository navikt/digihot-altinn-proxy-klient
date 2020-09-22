package no.nav.digihot.altinn.proxy.klient

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlient.Companion.CONSUMER_ID_HEADER_NAME
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlient.Companion.CORRELATION_ID_HEADER_NAME
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlient.Companion.PROXY_ENDEPUNKT_API_ORGANISASJONER
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlient.Companion.QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER
import no.nav.digihot.altinn.proxy.klient.error.ProxyError
import no.nav.digihot.altinn.proxy.klient.model.AltinnReportee
import org.apache.http.HttpStatus

class AltinnProxyKlientIntegrationTestUtils {

    companion object {
        const val NON_EMPTY_STRING_REGEX = "^(?!\\s*\$).+"
        const val PORT: Int = 1331
        const val DIGIHOT_SERVICE_CODE = "5614"
        const val DIGIHOT_SERVICE_EDITION = "1"
        const val INVALID_SERVICE_CODE = "9999"
        const val TOP_VALUE = "500"

        fun `altinn-proxy returnerer 200 OK og en liste med AltinnReportees`(
                serviceCode: String?,
                serviceEdition: String?,
                antallReportees: Int = 2,
                skip: String = "0",
                medFilter: Boolean = true
        ): MappingBuilder {

            val queryParametre = mutableMapOf(
                    "top" to equalTo(TOP_VALUE),
                    "skip" to equalTo(skip)
            )

            if (serviceCode != null) queryParametre["serviceCode"] = equalTo(serviceCode)
            if (serviceEdition != null) queryParametre["serviceEdition"] = equalTo(serviceEdition)
            if (medFilter) queryParametre["filter"] = equalTo(QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER)

            return get(urlPathEqualTo("/proxy$PROXY_ENDEPUNKT_API_ORGANISASJONER"))
                    .withHeader("Accept", equalTo("application/json"))
                    .withHeader("Authorization", matching(NON_EMPTY_STRING_REGEX))
                    .withHeader(CORRELATION_ID_HEADER_NAME, matching(NON_EMPTY_STRING_REGEX))
                    .withHeader(CONSUMER_ID_HEADER_NAME, matching(NON_EMPTY_STRING_REGEX))

                    .withQueryParams(queryParametre)
                    .willReturn(`200 response med en liste av reportees`(antallReportees))
        }

        fun `altinn-proxy returnerer en feil av type 'httpStatus' med 'kilde' og 'melding' i response body`(
                serviceCode: String,
                serviceEdition: String,
                skip: String = "0",
                httpStatusKode: Int,
                kilde: ProxyError.Kilde,
                melding: String
        ): MappingBuilder {
            return get(urlPathEqualTo("/proxy$PROXY_ENDEPUNKT_API_ORGANISASJONER"))
                    .withHeader("Accept", equalTo("application/json"))
                    .withQueryParams(mapOf(
                            "serviceCode" to equalTo(serviceCode),
                            "serviceEdition" to equalTo(serviceEdition),
                            "top" to equalTo(TOP_VALUE),
                            "skip" to equalTo(skip),
                            "filter" to equalTo(QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER)
                    ))
                    .willReturn(aResponse()
                            .withStatus(httpStatusKode)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{" +
                                    "\"origin\": \"${kilde.verdi}\"," +
                                    "\"message\": \"${melding}\"}"
                            )
                    )
        }

        fun `altinn-proxy returnerer 500 uhåndtert feil`(
                serviceCode: String,
                serviceEdition: String
        ): MappingBuilder {
            return get(urlPathEqualTo("/proxy$PROXY_ENDEPUNKT_API_ORGANISASJONER"))
                    .withHeader("Accept", equalTo("application/json"))
                    .withQueryParams(mapOf(
                            "serviceCode" to equalTo(serviceCode),
                            "serviceEdition" to equalTo(serviceEdition),
                            "top" to equalTo(TOP_VALUE),
                            "skip" to equalTo("0"),
                            "filter" to equalTo(QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER)
                    ))
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{" +
                                    "\"status\": \"500\"," +
                                    "\"message\": \"Internal Server Error\"}"
                            )
                    )
        }

        fun `200 response med en liste av reportees`(antallReportees: Int = 2): ResponseDefinitionBuilder? {
            var reportees = emptyList<String>()
            if (antallReportees != 0) {
                reportees = mutableListOf(
                        "    {" +
                                "        \"Name\": \"BALLSTAD OG HAMARØY\"," +
                                "        \"Type\": \"Business\"," +
                                "        \"ParentOrganizationNumber\": \"811076112\"," +
                                "        \"OrganizationNumber\": \"811076732\"," +
                                "        \"OrganizationForm\": \"BEDR\"," +
                                "        \"Status\": \"Active\"" +
                                "    }",
                        "    {" +
                                "        \"Name\": \"BALLSTAD OG HORTEN\"," +
                                "        \"Type\": \"Enterprise\"," +
                                "        \"ParentOrganizationNumber\": null," +
                                "        \"OrganizationNumber\": \"811076112\"," +
                                "        \"OrganizationForm\": \"AS\"," +
                                "        \"Status\": \"Active\"" +
                                "    }"
                )
                reportees.addAll(generateAltinnReporteeJson(antallReportees - 2))
            }

            return aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(reportees.joinToString(prefix = "[", postfix = "]", separator = ","))
        }

        private fun generateAltinnReporteeJson(antall: Int): List<String> {
            return List(antall) { index ->
                getAltinnReporteeJson(AltinnReportee(
                        "name_$index",
                        "Enterprise",
                        "0",
                        "$index",
                        "AS",
                        "Active"
                ))
            }
        }

        private fun getAltinnReporteeJson(reportee: AltinnReportee): String {
            return "    {" +
                    "        \"Name\": \"${reportee.name}\"," +
                    "        \"Type\": \"${reportee.type}\"," +
                    "        \"ParentOrganizationNumber\": \"${reportee.parentOrganizationNumber}\"," +
                    "        \"OrganizationNumber\": \"${reportee.organizationNumber}\"," +
                    "        \"OrganizationForm\": \"${reportee.organizationForm}\"," +
                    "        \"Status\": \"${reportee.status}\"" +
                    "    }"
        }

        fun `altinn-proxy mottar riktig request`(
                serviceCode: String?,
                serviceEdition: String?,
                skip: String = "0",
                medFilter: Boolean = true
        ): RequestPatternBuilder {
            val request = getRequestedFor(urlPathEqualTo("/proxy$PROXY_ENDEPUNKT_API_ORGANISASJONER"))
                    .withHeader("Accept", containing("application/json"))
                    .withHeader("Accept", equalTo("application/json"))
                    .withHeader("Authorization", matching(NON_EMPTY_STRING_REGEX))
                    .withHeader(CORRELATION_ID_HEADER_NAME, matching(NON_EMPTY_STRING_REGEX))
                    .withHeader(CONSUMER_ID_HEADER_NAME, matching(NON_EMPTY_STRING_REGEX))
                    .withQueryParam("top", equalTo(TOP_VALUE))
                    .withQueryParam("skip", equalTo(skip))

            if (serviceCode != null) request.withQueryParam("serviceCode", equalTo(serviceCode))
            if (serviceEdition != null) request.withQueryParam("serviceEdition", equalTo(serviceEdition))
            if (medFilter) request.withQueryParam("filter", equalTo(QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER))

            return request
        }

        fun `altinn-proxy returnerer 502 Bad Gateway`(
                serviceCode: String,
                serviceEdition: String
        ): MappingBuilder {
            return get(urlPathEqualTo("/proxy$PROXY_ENDEPUNKT_API_ORGANISASJONER"))
                    .withHeader("Accept", equalTo("application/json"))
                    .withQueryParams(mapOf(
                            "serviceCode" to equalTo(serviceCode),
                            "serviceEdition" to equalTo(serviceEdition),
                            "top" to equalTo(TOP_VALUE),
                            "skip" to equalTo("0"),
                            "filter" to equalTo(QUERY_PARAM_FILTER_AKTIVE_BEDRIFTER)
                    ))
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.SC_BAD_GATEWAY)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{" +
                                    "\"status\": \"502\"," +
                                    "\"message\": \"Bad Gateway\"}"
                            )
                    )
        }

    }
}
