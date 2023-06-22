package app.softwork.sqldelight.postgresdriver

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
internal class BinaryDecoder(
    override val serializersModule: SerializersModule,
    private val bytes: ByteArray
): Decoder, CompositeDecoder {
    private var index = 0
    
    private fun decodeNumber(size: Int): Number {
        var result = 0
        for (s in 0..size) {
            result += bytes[index].toInt() shl (s * 8) 
            index++
        }
        return result
    }
    
    override fun decodeSequentially(): Boolean = true
    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        TODO("Not yet implemented")
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
        decodeNumber(Short.SIZE_BYTES) as Short

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        TODO("Not yet implemented")
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        TODO("Not yet implemented")
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
        decodeNumber(Byte.SIZE_BYTES) as Byte

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        TODO("Not yet implemented")
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Nothing = error("Not supported")

    override fun decodeElementIndex(descriptor: SerialDescriptor): Nothing = error("Never called due to sequentially decoding")
    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Nothing = error("Not supported")

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int) = this

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decodeNumber(Int.SIZE_BYTES) as Int

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return decodeNumber(Long.SIZE_BYTES) as Long
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        TODO("Not yet implemented")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        TODO("Not yet implemented")
    }

    override fun decodeBoolean(): Boolean {
        TODO("Not yet implemented")
    }

    override fun decodeByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun decodeChar(): Char {
        TODO("Not yet implemented")
    }

    override fun decodeDouble(): Double {
        TODO("Not yet implemented")
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int { }
    override fun decodeFloat(): Float {
        TODO("Not yet implemented")
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented")
    }

    override fun decodeInt(): Int {
        TODO("Not yet implemented")
    }

    override fun decodeLong(): Long {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? {
        TODO("Not yet implemented")
    }

    override fun decodeShort(): Short {
        TODO("Not yet implemented")
    }

    override fun decodeString(): String {
        TODO("Not yet implemented")
    }
}
