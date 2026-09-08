package com.marketintelligence.ai.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

object LenientDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)
    override fun deserialize(decoder: Decoder): Double {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) element.doubleOrNull ?: 0.0 else 0.0
        } else {
            try { decoder.decodeDouble() } catch (_: Exception) { 0.0 }
        }
    }
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}

object LenientLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): Long {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.longOrNull ?: element.doubleOrNull?.toLong() ?: 0L
            } else 0L
        } else {
            try { decoder.decodeLong() } catch (_: Exception) { 0L }
        }
    }
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

@Serializable
data class CryptoData(
    val id: String = "",
    val symbol: String = "",
    val name: String = "",
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("current_price")
    val price: Double = 0.0,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("price_change_percentage_24h")
    val changePercent: Double = 0.0,
    @Serializable(with = LenientLongSerializer::class)
    @SerialName("market_cap")
    val marketCap: Long = 0L
)
