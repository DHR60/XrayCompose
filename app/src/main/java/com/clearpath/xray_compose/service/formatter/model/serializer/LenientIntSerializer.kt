package com.clearpath.xray_compose.service.formatter.model.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

class LenientIntSerializer : KSerializer<Int> {
    override val descriptor =
        PrimitiveSerialDescriptor("LenientIntSerializer", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer only works with JSON")
        val element = jsonDecoder.decodeJsonElement()

        return if (element is JsonPrimitive) {
            element.content.toIntOrNull()
                ?: throw SerializationException("Cannot Magnify '${element.content}' to Int")
        } else {
            throw SerializationException("Unexpected JSON element for Int")
        }
    }
}