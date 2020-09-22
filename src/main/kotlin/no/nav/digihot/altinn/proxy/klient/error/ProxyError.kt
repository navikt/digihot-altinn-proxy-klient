package no.nav.digihot.altinn.proxy.klient.error

abstract class ProxyError() {

    abstract val melding: String
    abstract val kilde: Kilde

    enum class Kilde(val verdi: String) {
        ALTINN("ALTINN"),
        ALTINN_PROXY("ALTINN_PROXY"),
        ALTINN_PROXY_KLIENT("ALTINN_PROXY_KLIENT")
    }
}

