package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.api.endpoints.JanitorAction

data class KafkaEventDTO(
    val referenceUUID: String,
    val navident: String,
    val eventUUID: String,
    val personident: String,
    val action: JanitorAction,
)
