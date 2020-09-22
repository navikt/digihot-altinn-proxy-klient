package no.nav.digihot.altinn.proxy.klient

import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlient.Companion.PROXY_ENDEPUNKT_API_ORGANISASJONER
import no.nav.digihot.altinn.proxy.klient.AltinnProxyKlient.Companion.getAltinnProxyURL
import org.junit.Assert.*
import org.junit.Test

class AltinnProxyKlientTest {

    @Test
    fun `getAltinnProxyURL() fjerner trailing slash på base path`() {
        assertEquals("http://altinn.proxy$PROXY_ENDEPUNKT_API_ORGANISASJONER",
                getAltinnProxyURL("http://altinn.proxy/", PROXY_ENDEPUNKT_API_ORGANISASJONER))
    }

}
