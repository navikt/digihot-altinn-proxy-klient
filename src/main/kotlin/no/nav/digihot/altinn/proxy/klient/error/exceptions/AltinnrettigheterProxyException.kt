package no.nav.digihot.altinn.proxy.klient.error.exceptions

import no.nav.digihot.altinn.proxy.klient.error.ProxyError
import java.lang.RuntimeException

class AltinnrettigheterProxyException(proxyError: ProxyError)
    : RuntimeException(proxyError.melding)