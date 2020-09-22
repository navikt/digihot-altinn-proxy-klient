package no.nav.digihot.altinn.proxy.klient.error.exceptions

import java.lang.RuntimeException

class AltinnProxyKlientException(melding: String, exception: Exception)
    : RuntimeException(melding, exception)