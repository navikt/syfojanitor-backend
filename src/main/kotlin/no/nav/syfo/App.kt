package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.clients.wellknown.getWellKnown
import no.nav.syfo.infrastructure.database.applicationDatabase
import no.nav.syfo.infrastructure.database.databaseModule
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState()
    val environment = Environment()
    val logger = LoggerFactory.getLogger("ktor.application")

    val wellKnownInternalAzureAD = getWellKnown(
        wellKnownUrl = environment.azure.appWellKnownUrl
    )
    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure
    )
    val veilederTilgangskontrollClient =
        VeilederTilgangskontrollClient(
            azureAdClient = azureAdClient,
            clientEnvironment = environment.clients.istilgangskontroll
        )
    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        pdlEnvironment = environment.clients.pdl,
    )
    val applicationEngineEnvironment =
        applicationEngineEnvironment {
            log = logger
            config = HoconApplicationConfig(ConfigFactory.load())
            connector {
                port = applicationPort
            }
            module {
                databaseModule(
                    databaseEnvironment = environment.database,
                )
                apiModule(
                    applicationState = applicationState,
                    environment = environment,
                    wellKnownInternalAzureAD = wellKnownInternalAzureAD,
                    database = applicationDatabase,
                    veilederTilgangskontrollClient = veilederTilgangskontrollClient,
                )
            }
        }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        applicationState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")
    }

    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment
    )

    Runtime.getRuntime().addShutdownHook(
        Thread { server.stop(10, 10, TimeUnit.SECONDS) }
    )

    server.start(wait = true)
}
