package no.nav.digihot.altinn.proxy.klient

data class AltinnrettigheterProxyKlientConfig(
        val proxy: ProxyConfig,
        val altinn: AltinnConfig
)

data class AltinnConfig(
        val url: String,
        val altinnApiKey: String,
        val altinnApiGwApiKey: String
)

data class ProxyConfig(
        val consumerId: String,
        val url: String
)