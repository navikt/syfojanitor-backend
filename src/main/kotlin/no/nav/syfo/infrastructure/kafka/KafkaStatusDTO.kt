package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.api.endpoints.JanitorStatus

data class KafkaStatusDTO(
    val eventUUID: String,
    val status: JanitorStatus,
)
