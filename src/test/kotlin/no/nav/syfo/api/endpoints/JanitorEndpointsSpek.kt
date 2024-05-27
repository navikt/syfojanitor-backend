package no.nav.syfo.api.endpoints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.domain.Event
import no.nav.syfo.generateJWTNavIdent
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.EventRepository
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.kafka.KafkaEventDTO
import no.nav.syfo.util.configure
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID
import java.util.concurrent.Future

object JanitorEndpointsSpek : Spek({

    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe(JanitorEndpointsSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val kafkaProducer = mockk<KafkaProducer<String, KafkaEventDTO>>(relaxed = true)

            application.routing {
                install(ContentNegotiation) {
                    jackson { configure() }
                }
                registerJanitorEndpoints(
                    eventRepository = EventRepository(database),
                    kafkaProducer = kafkaProducer,
                )
            }

            beforeEachTest {
                clearAllMocks()
                coEvery { kafkaProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
                database.dropData()
            }

            val validToken = generateJWTNavIdent(
                externalMockEnvironment.environment.azure.appClientId,
                externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                VEILEDER_IDENT,
            )
            val janitorRequestDTO = JanitorRequestDTO(
                personident = UserConstants.PERSONIDENT.value,
                referenceUUID = UUID.randomUUID().toString(),
                action = JanitorAction.LUKK_DIALOGMOTE,
                description = "Lukker dialogmote",
            )
            val url = "/api/v1/janitor"

            it("Returns CREATED on POST successfull janitor request") {
                with(
                    handleRequest(HttpMethod.Post, url) {
                        addHeader(Authorization, bearerHeader(validToken))
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(objectMapper.writeValueAsString(janitorRequestDTO))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.Created
                }
            }

            it("Returns created event on GET") {
                with(
                    handleRequest(HttpMethod.Post, url) {
                        addHeader(Authorization, bearerHeader(validToken))
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(objectMapper.writeValueAsString(janitorRequestDTO))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.Created
                }
                with(
                    handleRequest(HttpMethod.Get, url) {
                        addHeader(Authorization, bearerHeader(validToken))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK
                    val events = objectMapper.readValue<List<Event>>(response.content!!)
                    events.size shouldBeEqualTo 1
                    events[0].navident shouldBeEqualTo VEILEDER_IDENT
                    events[0].personident shouldBeEqualTo UserConstants.PERSONIDENT
                    events[0].action shouldBeEqualTo JanitorAction.LUKK_DIALOGMOTE
                    events[0].description shouldBeEqualTo "Lukker dialogmote"
                    events[0].status shouldBeEqualTo JanitorStatus.CREATED
                }
            }
        }
    }
})
