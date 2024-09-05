package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.domain.Event
import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.database.EventRepository
import no.nav.syfo.infrastructure.kafka.KafkaEventDTO
import no.nav.syfo.util.getNAVIdent
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.*

const val janitorApiBasePath = "/api/v1/janitor"

fun Route.registerJanitorEndpoints(
    eventRepository: EventRepository,
    kafkaProducer: KafkaProducer<String, KafkaEventDTO>,
) {
    route(janitorApiBasePath) {
        post {
            val requestDTO = call.receive<JanitorRequestDTO>()
            val navident = call.getNAVIdent()
            val event = Event(
                uuid = UUID.randomUUID(),
                referenceUUID = UUID.fromString(requestDTO.referenceUUID),
                personident = Personident(requestDTO.personident),
                navident = navident,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                action = requestDTO.action,
                description = requestDTO.description,
                status = JanitorStatus.CREATED,
            )

            eventRepository.createEvent(event)
            kafkaProducer.send(
                ProducerRecord(
                    "teamsykefravr.syfojanitor-event",
                    event.uuid.toString(),
                    KafkaEventDTO(
                        referenceUUID = requestDTO.referenceUUID,
                        navident = navident,
                        eventUUID = event.uuid.toString(),
                        personident = requestDTO.personident,
                        action = requestDTO.action,
                    )
                )
            ).also { it.get() }

            call.respond(HttpStatusCode.Created)
        }

        get {
            call.respond(eventRepository.getEvents())
        }
    }
}

data class JanitorRequestDTO(
    val referenceUUID: String,
    val personident: String,
    val action: JanitorAction,
    val description: String,
)

enum class JanitorAction {
    LUKK_DIALOGMOTE
}

enum class JanitorStatus {
    CREATED,
    OK,
    FAILED,
}
