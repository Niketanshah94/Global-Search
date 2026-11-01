package com.quickfind.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Serializes an [IntRange] as `[first, last]`. */
object IntRangeSerializer : KSerializer<IntRange> {
    private val delegate = IntArraySerializer()

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor("IntRange", delegate.descriptor)

    override fun serialize(encoder: Encoder, value: IntRange) {
        encoder.encodeSerializableValue(delegate, intArrayOf(value.first, value.last))
    }

    override fun deserialize(decoder: Decoder): IntRange {
        val array = decoder.decodeSerializableValue(delegate)
        return array[0]..array[1]
    }
}
