package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.ApplicationState
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.launchBackgroundTask
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

class KafkaStatusConsumer(
    private val kafkaEnvironment: KafkaEnvironment,
    private val applicationState: ApplicationState,
    private val database: DatabaseInterface
) {

    fun launch() {
        launchBackgroundTask(
            applicationState = applicationState
        ) {
            logger.info("Launching ${this::class.java.simpleName}")

            val kafkaConsumer = KafkaConsumer<String, KafkaStatusDTO>(
                kafkaAivenConsumerConfig<KafkaStatusDTODeserializer>(
                    kafkaEnvironment = kafkaEnvironment,
                )
            )

            kafkaConsumer.subscribe(
                listOf(TOPIC)
            )

            while (applicationState.ready) {
                val records = kafkaConsumer.poll(Duration.ofMillis(1000))
                if (records.count() > 0) {
                    database.connection.use { connection ->
                        records.forEach { record ->
                            val kafkaStatusDTO = record.value()
                            logger.info("Received record: ${kafkaStatusDTO.eventUUID} with status: ${kafkaStatusDTO.status}")
                            connection.prepareStatement(
                                """
                                UPDATE EVENT SET status = ?, updated_at = ? WHERE uuid = ?
                                """.trimMargin()
                            ).use {
                                it.setString(1, kafkaStatusDTO.status.name)
                                it.setObject(2, OffsetDateTime.now())
                                it.setString(3, kafkaStatusDTO.eventUUID)
                                it.executeUpdate()
                            }
                        }
                        connection.commit()
                    }
                    kafkaConsumer.commitSync()
                }
            }
        }
    }

    companion object {
        private const val TOPIC = "teamsykefravr.syfojanitor-status"
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
