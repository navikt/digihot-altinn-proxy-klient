package no.nav.digihot.altinn.proxy.klient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.DIGIHOT_SERVICE_CODE
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.DIGIHOT_SERVICE_EDITION
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.`altinn-proxy mottar riktig request`
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlientIntegrationTestUtils.Companion.`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`
import no.nav.digihot.altinn.proxy.klient.model.SelvbetjeningToken
import no.nav.digihot.altinn.proxy.klient.model.ServiceCode
import no.nav.digihot.altinn.proxy.klient.model.ServiceEdition
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue


class AltinnProxyKlientIntegrationTest {

    private val klient: AltinnProxyKlient = AltinnProxyKlient("klient-applikasjon", "http://localhost:$PORT/proxy")

    @Before
    fun setUp() {
        wireMockServer.resetAll()
    }

    // API signature tester
    @Test
    fun `hentOrganisasjoner() basert på rettighet kaller AltinnProxy med riktige parametre og returnerer en liste av Altinn reportees`() {
        wireMockServer.stubFor(`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`(
                DIGIHOT_SERVICE_CODE,
                DIGIHOT_SERVICE_EDITION)
        )

        val organisasjoner = klient.hentOrganisasjoner(
                selvbetjeningToken,
                ServiceCode(DIGIHOT_SERVICE_CODE),
                ServiceEdition(DIGIHOT_SERVICE_EDITION),
                true
        )

        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION))
        assertTrue { organisasjoner.size == 2 }
    }


    // Tester som beviser at klient kan gjøre flere kall for å hente alle organisasjoner

    @Test
    fun `hentOrganisasjoner() kaller AltinnProxy flere ganger hvis bruker har tilgang til flere enn 499 virksomheter`() {
        wireMockServer.stubFor(`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`(
                DIGIHOT_SERVICE_CODE,
                DIGIHOT_SERVICE_EDITION,
                500)
        )
        wireMockServer.stubFor(`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`(
                DIGIHOT_SERVICE_CODE,
                DIGIHOT_SERVICE_EDITION,
                0,
                "500")
        )

        val organisasjoner = klient.hentOrganisasjoner(
                selvbetjeningToken,
                ServiceCode(DIGIHOT_SERVICE_CODE),
                ServiceEdition(DIGIHOT_SERVICE_EDITION),
                true
        )

        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION, "0"))
        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION, "500"))
        assertTrue { organisasjoner.size == 500 }
    }

    @Test
    fun `hentOrganisasjoner() skal hente alle virksomhetene hvis bruker har tilgang til flere enn 500 virksomheter`() {
        wireMockServer.stubFor(`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`(
                DIGIHOT_SERVICE_CODE,
                DIGIHOT_SERVICE_EDITION,
                500)
        )
        wireMockServer.stubFor(`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`(
                DIGIHOT_SERVICE_CODE,
                DIGIHOT_SERVICE_EDITION,
                500,
                "500")
        )
        wireMockServer.stubFor(`altinn-proxy returnerer 200 OK og en liste med AltinnReportees`(
                DIGIHOT_SERVICE_CODE,
                DIGIHOT_SERVICE_EDITION,
                321,
                "1000")
        )

        val organisasjoner = klient.hentOrganisasjoner(
                selvbetjeningToken,
                ServiceCode(DIGIHOT_SERVICE_CODE),
                ServiceEdition(DIGIHOT_SERVICE_EDITION),
                true
        )

        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION, "0"))
        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION, "500"))
        wireMockServer.verify(`altinn-proxy mottar riktig request`(DIGIHOT_SERVICE_CODE, DIGIHOT_SERVICE_EDITION, "1000"))
        assertTrue { organisasjoner.size == 1321 }
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
