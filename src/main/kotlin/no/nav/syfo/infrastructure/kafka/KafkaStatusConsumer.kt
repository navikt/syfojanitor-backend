package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.ApplicationState
import no.nav.syfo.infrastructure.database.EventRepository
import no.nav.syfo.launchBackgroundTask
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaStatusConsumer(
    private val kafkaEnvironment: KafkaEnvironment,
    private val applicationState: ApplicationState,
    private val eventRepository: EventRepository,
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
                    records.forEach { record ->
                        val kafkaStatusDTO = record.value()
                        logger.info("Received record: ${kafkaStatusDTO.eventUUID} with status: ${kafkaStatusDTO.status}")
                        eventRepository.updateStatus(kafkaStatusDTO.eventUUID, kafkaStatusDTO.status)
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
