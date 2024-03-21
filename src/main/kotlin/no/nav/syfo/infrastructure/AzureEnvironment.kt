package no.nav.syfo.infrastructure

data class AzureEnvironment(
    val appClientId: String,
    val appClientSecret: String,
    val appWellKnownUrl: String,
    val openidConfigTokenEndpoint: String
)
