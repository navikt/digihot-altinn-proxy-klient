package no.nav.digihot.altinn.proxy.klient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.DIGIHOT_SERVICE_CODE
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.DIGIHOT_SERVICE_EDITION
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.INVALID_SERVICE_CODE
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.`altinn-proxy mottar riktig request`
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.`altinn-proxy returnerer 500 uhåndtert feil`
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.`altinn-proxy returnerer 502 Bad Gateway`
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.`altinn-proxy returnerer en feil av type 'httpStatus' med 'kilde' og 'melding' i response body`
import no.nav.digihot.altinn.proxy.klient.error.ProxyError
import no.nav.digihot.altinn.proxy.klient.model.SelvbetjeningToken
import no.nav.digihot.altinn.proxy.klient.model.ServiceCode
import no.nav.digihot.altinn.proxy.klient.model.ServiceEdition
import org.apache.http.HttpStatus
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail


class AltinnProxyKlientFeilhaandteringIntegrationTest {

    private val klient: AltinnProxyKlient = AltinnProxyKlient("klient-applikasjon", "http://localhost:$PORT/proxy")

    @Before
    fun setUp() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentOrganisasjoner() kaster Exception etter at proxy svarer med intern feil`() {
        wireMockServer.stubFor(
                `altinn-proxy returnerer en feil av type 'httpStatus' med 'kilde' og 'melding' i response body`(
                        DIGIHOT_SERVICE_CODE,
                        DIGIHOT_SERVICE_EDITION,
                        "0",
                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        ProxyError.Kilde.ALTINN_PROXY,
                        "500: Internal server error"
                )
        )

        try {
            klient.hentOrganisasjoner(
                    selvbetjeningToken,
                    ServiceCode(DIGIHOT_SERVICE_CODE),
                    ServiceEdition(DIGIHOT_SERVICE_EDITION),
                    true
            )
            fail("Skulle ha fått en exception")
        } catch (e: Exception) {
            assertEquals("500: Internal server error", e.message)
        }
        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION))
    }

    @Test
    fun `hentOrganisasjoner() kaster en exception dersom Altinn svarer med feil til proxy`() {
        wireMockServer.stubFor(
                `altinn-proxy returnerer en feil av type 'httpStatus' med 'kilde' og 'melding' i response body`(
                        INVALID_SERVICE_CODE,
                        DIGIHOT_SERVICE_EDITION,
                        "0",
                        HttpStatus.SC_BAD_GATEWAY,
                        ProxyError.Kilde.ALTINN,
                        "400: The ServiceCode=9999 and ServiceEditionCode=1 are either invalid or non-existing"
                )
        )

        try {
            klient.hentOrganisasjoner(
                    selvbetjeningToken,
                    ServiceCode(INVALID_SERVICE_CODE),
                    ServiceEdition(DIGIHOT_SERVICE_EDITION),
                    true
            )
            fail("Skulle ha fått en exception")
        } catch (e: Exception) {
            assertEquals("400: The ServiceCode=9999 and ServiceEditionCode=1 " +
                    "are either invalid or non-existing", e.message)
        }

        wireMockServer.verify(`altinn-proxy mottar riktig request`(INVALID_SERVICE_CODE, DIGIHOT_SERVICE_EDITION))
        val alleRequestTilAltinn = wireMockServer.findAll(getRequestedFor(urlMatching("/altinn/.*")))
        assertTrue(alleRequestTilAltinn.isEmpty())
    }

    @Test
    fun `hentOrganisasjoner() kaster en exception hvis proxy svarer med uhåndtert feil`() {

        wireMockServer.stubFor(`altinn-proxy returnerer 500 uhåndtert feil`(
                DIGIHOT_SERVICE_CODE,
                DIGIHOT_SERVICE_EDITION)
        )

        try {
            klient.hentOrganisasjoner(
                    selvbetjeningToken,
                    ServiceCode(DIGIHOT_SERVICE_CODE),
                    ServiceEdition(DIGIHOT_SERVICE_EDITION),
                    true
            )
            fail("Skulle ha fått en exception")
        } catch (e: Exception) {
            assertEquals("Uhåndtert feil i proxy", e.message)
        }

        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION))
    }

    @Test
    fun `hentOrganisasjoner() kaster exception naar ingen tjeneste svarer`() {
        val klientMedProxyOgAltinnSomAldriSvarer = AltinnProxyKlient("klient-applikasjon", "http://localhost:13456/proxy-url-som-aldri-svarer")

        try {
            klientMedProxyOgAltinnSomAldriSvarer.hentOrganisasjoner(
                    selvbetjeningToken,
                    ServiceCode(INVALID_SERVICE_CODE),
                    ServiceEdition(DIGIHOT_SERVICE_EDITION),
                    true
            )
            fail("Skulle ha fått en exception")
        } catch (e: Exception) {
            assertTrue(
                    (e.message ?: "Ingen melding").startsWith("Uhåndtert feil i proxy")
            )
        }
    }

    companion object {
        const val PORT: Int = 1331
        private lateinit var wireMockServer: WireMockServer
        val selvbetjeningToken = SelvbetjeningToken("dette_er_ikke_en_ekte_idToken")

        @BeforeClass
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig()
                    .port(PORT)
                    .notifier(ConsoleNotifier(false)))
            wireMockServer.start()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }
}
