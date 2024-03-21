package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

class KafkaStatusDTODeserializer : Deserializer<KafkaStatusDTO> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaStatusDTO =
        mapper.readValue(data, KafkaStatusDTO::class.java)
}
