package no.nav.syfo

import no.nav.syfo.infrastructure.clients.wellknown.WellKnown
import no.nav.syfo.infrastructure.database.TestDatabase
import java.nio.file.Paths

fun wellKnownInternalAzureAD(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString()
    )
}

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val environment = testEnvironment()
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
