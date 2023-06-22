package app.softwork.sqldelight.postgresdriver

import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
internal class SizeBinary(
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : BinaryFormat {
    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val decoder = BinaryDecoder(serializersModule, bytes)
        return deserializer.deserialize(decoder)
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        return encodeToByteReadPacket(serializer, value).readBytes()
    }
    
    fun <T> encodeToByteReadPacket(serializer: SerializationStrategy<T>, value: T): ByteReadPacket {
        val encoder = SizeBinaryEncoder(serializersModule)
        serializer.serialize(encoder, value)
        return encoder.getPacket()
    }
}

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
internal annotation class SizePlaceholder

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
internal annotation class IgnoreSize
