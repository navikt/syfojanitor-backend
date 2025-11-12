package no.nav.syfo.api.endpoints

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
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
import no.nav.syfo.infrastructure.database.EventRepository
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.kafka.KafkaEventDTO
import no.nav.syfo.util.configure
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Future

class JanitorEndpointsTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaProducer = mockk<KafkaProducer<String, KafkaEventDTO>>(relaxed = true)

    private fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
        application {
            routing {
                install(ContentNegotiation) {
                    jackson { configure() }
                }
                registerJanitorEndpoints(
                    eventRepository = EventRepository(database),
                    kafkaProducer = kafkaProducer,
                )
            }
        }
        return createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                jackson { configure() }
            }
        }
    }

    @BeforeEach
    fun setup() {
        clearAllMocks()
        coEvery { kafkaProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        database.dropData()
    }

    private val validToken = generateJWTNavIdent(
        externalMockEnvironment.environment.azure.appClientId,
        externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        VEILEDER_IDENT,
    )
    private val janitorRequestDTO = JanitorRequestDTO(
        personident = UserConstants.PERSONIDENT.value,
        referenceUUID = UUID.randomUUID().toString(),
        action = JanitorAction.LUKK_DIALOGMOTE,
        description = "Lukker dialogmote",
    )
    private val url = "/api/v1/janitor"

    @Test
    fun `Returns CREATED on POST successful janitor request`() {
        testApplication {
            val client = setupApiAndClient()
            val response = client.post(url) {
                bearerAuth(validToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(janitorRequestDTO)
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `Returns created event on GET`() {
        testApplication {
            val client = setupApiAndClient()
            val postResponse = client.post(url) {
                bearerAuth(validToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(janitorRequestDTO)
            }
            assertEquals(HttpStatusCode.Created, postResponse.status)

            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val events = response.body<List<Event>>()
            assertEquals(1, events.size)
            assertEquals(VEILEDER_IDENT, events[0].navident)
            assertEquals(UserConstants.PERSONIDENT, events[0].personident)
            assertEquals(JanitorAction.LUKK_DIALOGMOTE, events[0].action)
            assertEquals("Lukker dialogmote", events[0].description)
            assertEquals(JanitorStatus.CREATED, events[0].status)
        }
    }
}
