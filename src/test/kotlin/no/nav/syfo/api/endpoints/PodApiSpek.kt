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
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.TestDatabase
import no.nav.syfo.infrastructure.database.TestDatabaseNotResponding
import no.nav.syfo.util.configure
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PodApiSpek : Spek({
    describe("liveness and readiness checks") {
        val databaseOk = TestDatabase()
        val databaseNotResponding = TestDatabaseNotResponding()

        fun ApplicationTestBuilder.setupApiAndClient(
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

        it("Returns ok on is_alive and is_ready") {
            testApplication {
                val client = setupApiAndClient(
                    applicationState = ApplicationState(
                        alive = true,
                        ready = true
                    ),
                )
                val responseLiveness = client.get("/internal/is_alive") {}
                responseLiveness.status shouldBeEqualTo HttpStatusCode.OK
                responseLiveness.body<String>() shouldNotBeEqualTo null

                val responseReady = client.get("/internal/is_ready") {}
                responseReady.status shouldBeEqualTo HttpStatusCode.OK
                responseReady.body<String>() shouldNotBeEqualTo null
            }
        }
        it("Returns internal server error when liveness and readiness check fails") {
            testApplication {
                val client = setupApiAndClient(
                    applicationState = ApplicationState(
                        alive = false,
                        ready = false
                    ),
                )
                val responseLiveness = client.get("/internal/is_alive") {}
                responseLiveness.status shouldBeEqualTo HttpStatusCode.InternalServerError
                responseLiveness.body<String>() shouldNotBeEqualTo null

                val responseReady = client.get("/internal/is_ready") {}
                responseReady.status shouldBeEqualTo HttpStatusCode.InternalServerError
                responseReady.body<String>() shouldNotBeEqualTo null
            }
        }

        it("Successful liveness and unsuccessful readiness checks when database not working") {
            testApplication {
                val client = setupApiAndClient(
                    applicationState = ApplicationState(
                        alive = true,
                        ready = true
                    ),
                    database = databaseNotResponding,
                )
                val responseLiveness = client.get("/internal/is_alive") {}
                responseLiveness.status shouldBeEqualTo HttpStatusCode.OK
                responseLiveness.body<String>() shouldNotBeEqualTo null

                val responseReady = client.get("/internal/is_ready") {}
                responseReady.status shouldBeEqualTo HttpStatusCode.InternalServerError
                responseReady.body<String>() shouldNotBeEqualTo null
            }
        }
    }
})
