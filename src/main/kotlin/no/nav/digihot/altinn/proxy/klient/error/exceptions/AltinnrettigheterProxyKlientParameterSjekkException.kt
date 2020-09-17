package no.nav.digihot.altinn.proxy.klient.error.exceptions

import java.lang.RuntimeException

class AltinnrettigheterProxyKlientParameterSjekkException(melding: String)
    : RuntimeException(melding)