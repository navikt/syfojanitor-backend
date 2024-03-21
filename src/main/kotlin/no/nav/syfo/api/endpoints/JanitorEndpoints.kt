package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getNAVIdent
import java.time.OffsetDateTime
import java.util.*

const val janitorApiBasePath = "/api/v1/janitor"

fun Route.registerJanitorEndpoints(
    database: DatabaseInterface,
) {
    route(janitorApiBasePath) {
        post {
            val requestDTO = call.receive<JanitorRequestDTO>()

            val navIdent = call.getNAVIdent()
            val callId = call.getCallId()
            val now = OffsetDateTime.now()
            val status = "CREATED"

            database.connection.use {
                it.prepareStatement(
                    """INSERT INTO event (
    id, uuid, reference_uuid, personident, navident, created_at, updated_at, type, description, status             TEXT        NOT NULL                    
                    ) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin()
                ).use { ps ->
                    ps.setString(1, UUID.randomUUID().toString())
                    ps.setString(2, requestDTO.uuid)
                    ps.setString(3, requestDTO.personident)
                    ps.setString(4, navIdent)
                    ps.setObject(5, now)
                    ps.setObject(6, now)
                    ps.setString(7, requestDTO.type)
                    ps.setString(8, requestDTO.description)
                    ps.setString(9, status)
                }
                it.commit()
            }
            call.respond(HttpStatusCode.Created)
        }
    }
}

data class JanitorRequestDTO(
    val uuid: String,
    val personident: String,
    val type: String,
    val description: String,
)
