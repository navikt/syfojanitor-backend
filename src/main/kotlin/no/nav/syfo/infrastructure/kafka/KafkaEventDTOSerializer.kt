package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Serializer

class KafkaEventDTOSerializer : Serializer<KafkaEventDTO> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KafkaEventDTO?): ByteArray =
        mapper.writeValueAsBytes(data)
}
