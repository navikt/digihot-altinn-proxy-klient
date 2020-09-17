package no.nav.digihot.altinn.proxy.klient.error.exceptions

import no.nav.digihot.altinn.proxy.klient.error.ProxyError
import java.lang.RuntimeException

class AltinnException(proxyError: ProxyError)
    : RuntimeException(proxyError.melding)