package no.nav.syfo

import no.nav.syfo.infrastructure.AzureEnvironment
import no.nav.syfo.infrastructure.database.DatabaseEnvironment

fun testEnvironment() = Environment(
    database = DatabaseEnvironment(
        host = "localhost",
        port = "5432",
        name = "isaktivitetskrav_dev",
        username = "username",
        password = "password",
    ),
    azure = AzureEnvironment(
        appClientId = "syfojanitor-client-id",
        appClientSecret = "syfojanitor-secret",
        appWellKnownUrl = "wellknown",
        openidConfigTokenEndpoint = "azureOpenIdTokenEndpoint",
    ),
    electorPath = "electorPath",
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
