package no.nav.syfo.infrastructure.kafka

data class KafkaEventDTO(
    val referenceUUID: String,
    val navident: String,
    val eventUUID: String,
    val personident: String,
    val action: String,
)
