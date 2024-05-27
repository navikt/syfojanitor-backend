package no.nav.syfo.infrastructure.database

import no.nav.syfo.api.endpoints.JanitorAction
import no.nav.syfo.api.endpoints.JanitorStatus
import no.nav.syfo.domain.Event
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.toOffsetDateTimeUTC
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*

class EventRepository(private val database: DatabaseInterface) {

    fun createEvent(event: Event) = database.connection.use { connection ->
        connection.prepareStatement(CREATE_EVENT_QUERY).use {
            it.setString(1, event.uuid.toString())
            it.setString(2, event.referenceUUID.toString())
            it.setString(3, event.personident.value)
            it.setString(4, event.navident)
            it.setObject(5, event.createdAt.toOffsetDateTimeUTC())
            it.setObject(6, event.updatedAt.toOffsetDateTimeUTC())
            it.setString(7, event.action.name)
            it.setString(8, event.description)
            it.setString(9, event.status.name)
            it.executeUpdate()
        }
        connection.commit()
    }

    fun updateStatus(eventUUID: String, status: JanitorStatus) = database.connection.use { connection ->
        connection.prepareStatement(UPDATE_EVENT_QUERY).use {
            it.setString(1, status.name)
            it.setObject(2, OffsetDateTime.now())
            it.setString(3, eventUUID)
            it.executeUpdate()
        }
        connection.commit()
    }

    fun getEvents(): List<Event> = database.connection.use { connection ->
        connection.prepareStatement(GET_EVENTS_QUERY).use {
            it.executeQuery().toList { toEvent() }
        }
    }

    companion object {
        private const val CREATE_EVENT_QUERY =
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
            """

        private const val UPDATE_EVENT_QUERY =
            """
                UPDATE event SET status = ?, updated_at = ? WHERE uuid = ?
            """

        private const val GET_EVENTS_QUERY =
            """
                SELECT * FROM event ORDER BY created_at DESC
            """
    }

    private fun ResultSet.toEvent(): Event =
        Event(
            uuid = UUID.fromString(getString("uuid")),
            createdAt = getTimestamp("created_at").toLocalDateTime(),
            updatedAt = getTimestamp("updated_at").toLocalDateTime(),
            status = JanitorStatus.valueOf(getString("status")),
            action = JanitorAction.valueOf(getString("type")),
            description = getString("description"),
            referenceUUID = UUID.fromString(getString("reference_uuid")),
            personident = Personident(getString("personident")),
            navident = getString("navident")
        )
}
