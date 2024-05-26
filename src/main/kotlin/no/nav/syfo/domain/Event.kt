package no.nav.syfo.domain

import no.nav.syfo.api.endpoints.JanitorAction
import no.nav.syfo.api.endpoints.JanitorStatus
import java.time.LocalDateTime
import java.util.UUID

data class Event(
    val uuid: UUID,
    val referenceUUID: UUID,
    val personident: Personident,
    val navident: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val action: JanitorAction,
    val description: String,
    val status: JanitorStatus,
)
