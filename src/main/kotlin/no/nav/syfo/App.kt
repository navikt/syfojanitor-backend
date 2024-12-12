package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.infrastructure.clients.wellknown.getWellKnown
import no.nav.syfo.infrastructure.database.EventRepository
import no.nav.syfo.infrastructure.database.applicationDatabase
import no.nav.syfo.infrastructure.database.databaseModule
import no.nav.syfo.infrastructure.kafka.KafkaStatusConsumer
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

    lateinit var eventRepository: EventRepository

    val applicationEnvironment = applicationEnvironment {
        log = logger
        config = HoconApplicationConfig(ConfigFactory.load())
    }

    val server = embeddedServer(
        Netty,
        environment = applicationEnvironment,
        configure = {
            connector {
                port = applicationPort
            }
            connectionGroupSize = 8
            workerGroupSize = 8
            callGroupSize = 16
        },

        module = {
            databaseModule(
                databaseEnvironment = environment.database,
            )

            eventRepository = EventRepository(database = applicationDatabase)

            apiModule(
                applicationState = applicationState,
                environment = environment,
                wellKnownInternalAzureAD = wellKnownInternalAzureAD,
                database = applicationDatabase,
                eventRepository = eventRepository,
            )
            monitor.subscribe(ApplicationStarted) {
                applicationState.ready = true
                logger.info("Application is ready, running Java VM ${Runtime.version()}")
                val kafkaStatusConsumer = KafkaStatusConsumer(
                    eventRepository = eventRepository,
                    kafkaEnvironment = environment.kafka,
                    applicationState = applicationState
                )
                kafkaStatusConsumer.launch()
            }
        }
    )

    Runtime.getRuntime().addShutdownHook(
        Thread { server.stop(10, 10, TimeUnit.SECONDS) }
    )

    server.start(wait = true)
}
