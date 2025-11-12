package no.nav.syfo.api.endpoints

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.TestDatabaseNotResponding
import no.nav.syfo.util.configure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.Assertions.*

class PodApiTest {
    private val databaseOk = ExternalMockEnvironment.instance.database
    private val databaseNotResponding = TestDatabaseNotResponding()

    private fun ApplicationTestBuilder.setupApiAndClient(
        applicationState: ApplicationState,
        database: DatabaseInterface = databaseOk,
    ): HttpClient {
        application {
            routing {
                install(ContentNegotiation) {
                    jackson { configure() }
                }
                podEndpoints(
                    applicationState = applicationState,
                    database = database,
                )
            }
        }
        return createClient {}
    }

    @Test
    fun `Returns ok on is_alive and is_ready`() {
        testApplication {
            val client = setupApiAndClient(
                applicationState = ApplicationState(
                    alive = true,
                    ready = true
                ),
            )
            val responseLiveness = client.get("/internal/is_alive") {}
            assertEquals(HttpStatusCode.OK, responseLiveness.status)
            assertNotNull(responseLiveness.body())

            val responseReady = client.get("/internal/is_ready") {}
            assertEquals(HttpStatusCode.OK, responseReady.status)
            assertNotNull(responseReady.body())
        }
    }

    @Test
    fun `Returns internal server error when liveness and readiness check fails`() {
        testApplication {
            val client = setupApiAndClient(
                applicationState = ApplicationState(
                    alive = false,
                    ready = false
                ),
            )
            val responseLiveness = client.get("/internal/is_alive") {}
            assertEquals(HttpStatusCode.InternalServerError, responseLiveness.status)
            assertNotNull(responseLiveness.body())

            val responseReady = client.get("/internal/is_ready") {}
            assertEquals(HttpStatusCode.InternalServerError, responseReady.status)
            assertNotNull(responseReady.body())
        }
    }

    @Test
    fun `Successful liveness and unsuccessful readiness checks when database not working`() {
        testApplication {
            val client = setupApiAndClient(
                applicationState = ApplicationState(
                    alive = true,
                    ready = true
                ),
                database = databaseNotResponding,
            )
            val responseLiveness = client.get("/internal/is_alive") {}
            assertEquals(HttpStatusCode.OK, responseLiveness.status)
            assertNotNull(responseLiveness.body())

            val responseReady = client.get("/internal/is_ready") {}
            assertEquals(HttpStatusCode.InternalServerError, responseReady.status)
            assertNotNull(responseReady.body())
        }
    }
}
