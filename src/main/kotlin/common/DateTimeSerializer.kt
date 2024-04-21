package org.example.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

object DateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun deserialize(decoder: Decoder): LocalDateTime {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        TODO("Not yet implemented")
    }

}