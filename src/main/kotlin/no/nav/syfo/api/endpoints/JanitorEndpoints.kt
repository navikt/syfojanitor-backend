package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getNAVIdent

const val janitorApiBasePath = "/api/v1/janitor"

fun Route.registerJanitorEndpoints(
) {
    route(janitorApiBasePath) {
        post {
            val requestDTO = call.receive<JanitorRequestDTO>()

            val navIdent = call.getNAVIdent()
            val callId = call.getCallId()

            call.respond(HttpStatusCode.Created)
        }
    }
}

data class JanitorRequestDTO(
    val uuid: String,
    val personident: String,
    val serviceType: String,
    val description: String,
)
