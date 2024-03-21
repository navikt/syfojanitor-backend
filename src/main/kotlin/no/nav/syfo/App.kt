package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.infrastructure.clients.wellknown.getWellKnown
import no.nav.syfo.infrastructure.database.applicationDatabase
import no.nav.syfo.infrastructure.database.databaseModule
import no.nav.syfo.infrastructure.kafka.KafkaStatusDTO
import no.nav.syfo.infrastructure.kafka.KafkaStatusDTODeserializer
import no.nav.syfo.infrastructure.kafka.kafkaAivenConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState()
    val environment = Environment()
    val logger = LoggerFactory.getLogger("ktor.application")

    val wellKnownInternalAzureAD = getWellKnown(
        wellKnownUrl = environment.azure.appWellKnownUrl
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
                )
            }
        }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        applicationState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")
        launchBackgroundTask(
            applicationState = applicationState
        ) {
            val topic = "teamsykefravr.syfojanitor-status"
            logger.info("Launcing background task")

            val kafkaConsumer = KafkaConsumer<String, KafkaStatusDTO>(
                kafkaAivenConsumerConfig<KafkaStatusDTODeserializer>(
                    kafkaEnvironment = environment.kafka,
                )
            )

            kafkaConsumer.subscribe(
                listOf(topic)
            )

            while (applicationState.ready) {
                val records = kafkaConsumer.poll(Duration.ofMillis(1000))
                if (records.count() > 0) {
                    records.forEach { record ->
                        logger.info("Received record: ${record.value().eventUUID}")
                        // TODO: Update status in database
                    }
                    kafkaConsumer.commitSync()
                }
            }
        }
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
