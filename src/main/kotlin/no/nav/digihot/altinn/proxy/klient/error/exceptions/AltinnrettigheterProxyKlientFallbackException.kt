package no.nav.digihot.altinn.proxy.klient.error.exceptions

import java.lang.RuntimeException

class AltinnrettigheterProxyKlientFallbackException(melding: String, exception: Exception)
    : RuntimeException(melding, exception)