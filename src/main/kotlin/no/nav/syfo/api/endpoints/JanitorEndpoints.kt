package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.kafka.KafkaEventDTO
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getNAVIdent
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.OffsetDateTime
import java.util.*

const val janitorApiBasePath = "/api/v1/janitor"

fun Route.registerJanitorEndpoints(
    database: DatabaseInterface,
    kafkaProducer: KafkaProducer<String, KafkaEventDTO>,
) {
    route(janitorApiBasePath) {
        post {
            val requestDTO = call.receive<JanitorRequestDTO>()

            val navident = call.getNAVIdent()
            val callId = call.getCallId()
            val now = OffsetDateTime.now()
            val status = "CREATED"
            val eventUUID = UUID.randomUUID().toString()


            database.connection.use {
                it.prepareStatement(
                    """INSERT INTO event (
                        id, 
                        uuid, 
                        reference_uuid, 
                        personident, 
                        navident, 
                        created_at, 
                        updated_at, 
                        type, 
                        description, 
                        status                    
                    ) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin()
                ).use { ps ->
                    ps.setString(1, eventUUID)
                    ps.setString(2, requestDTO.referenceUUID)
                    ps.setString(3, requestDTO.personident)
                    ps.setString(4, navident)
                    ps.setObject(5, now)
                    ps.setObject(6, now)
                    ps.setString(7, requestDTO.action)
                    ps.setString(8, requestDTO.description)
                    ps.setString(9, status)
                }

                kafkaProducer.send(
                    ProducerRecord(
                        "teamsykefravr.syfojanitor-event",
                        eventUUID,
                        KafkaEventDTO(
                            referenceUUID = requestDTO.referenceUUID,
                            navident = navident,
                            eventUUID = eventUUID,
                            personident = requestDTO.personident,
                            action = requestDTO.action,
                        )
                    )
                ).get()

                it.commit()
            }
            call.respond(HttpStatusCode.Created)
        }
    }
}

data class JanitorRequestDTO(
    val referenceUUID: String,
    val personident: String,
    val action: String,
    val description: String,
)
