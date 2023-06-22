package app.softwork.sqldelight.postgresdriver

import io.ktor.utils.io.core.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlin.properties.Delegates

@ExperimentalSerializationApi
internal class SizeBinaryEncoder(override val serializersModule: SerializersModule) : Encoder, CompositeEncoder {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = this

    private var sizeOffset by Delegates.setOnce()
    private var ignoreSize = 0

    private val packet = BytePacketBuilder()

    fun getPacket(): ByteReadPacket {
        val size = packet.size
        val finalPacket = packet.build()
        
        return buildPacket { 
            writePacket(finalPacket, sizeOffset)
            writeInt(size)
            finalPacket.discardExact(sizeOffset + 4)
            writePacket(finalPacket)
        }
    }

    override fun encodeBoolean(value: Boolean) {
        if (value) {
            packet.writeByte(1)
        } else {
            packet.writeByte(1)
        }
    }

    override fun encodeByte(value: Byte) {
        packet.writeByte(value)
    }

    override fun encodeChar(value: Char) {
        packet.writeByte(value.code.toByte())
    }

    override fun encodeInt(value: Int) {
        packet.writeInt(value)
    }

    override fun encodeLong(value: Long) {
        packet.writeLong(value)
    }

    override fun encodeShort(value: Short) {
        packet.writeShort(value)
    }

    override fun encodeString(value: String) {
        packet.writeText(value)
    }

    override fun encodeInline(descriptor: SerialDescriptor) = this
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        if (value) {
            packet.writeByte(1)
        } else {
            packet.writeInt(0)
        }
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        packet.writeByte(value)
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        packet.writeByte(value.code.toByte())
    }

    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder = this

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        for (anno in descriptor.getElementAnnotations(index)) {
            if (anno is SizePlaceholder) {
                sizeOffset = packet.size
                break
            }
        }
        packet.writeInt(value)
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        packet.writeLong(value)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null) {
            serializer.serialize(this, value)
        }
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        serializer.serialize(this, value)
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        packet.writeShort(value)
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        packet.writeText(value)
    }

    override fun endStructure(descriptor: SerialDescriptor) {}

    override fun encodeDouble(value: Double): Nothing = error("Not supported")
    override fun encodeFloat(value: Float): Nothing = error("Not supported")
    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double): Nothing =
        error("Not supported")

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float): Nothing =
        error("Not supported")

    @ExperimentalSerializationApi
    override fun encodeNull(): Nothing = error("Not supported")
}
