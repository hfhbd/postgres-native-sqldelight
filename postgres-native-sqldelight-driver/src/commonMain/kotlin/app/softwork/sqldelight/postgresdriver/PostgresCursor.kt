package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import kotlinx.datetime.*
import kotlinx.uuid.UUID
import kotlinx.uuid.toUUID

public sealed class PostgresCursor(
    internal var result: PGResult
) : SqlCursor {
    abstract override fun next(): QueryResult.Value<Boolean>
    
    internal abstract val currentRowIndex: Int

    override fun getBoolean(index: Int): Boolean? = getString(index)?.toBoolean()

    override fun getBytes(index: Int): ByteArray? {
        val value = result[currentRowIndex, index] ?: return null
        require(value is Parameter.Bytes) {
            "Column $index of row $currentRowIndex isn't a Binary value."
        }
        return value.bytes?.fromHex()
    }

    private inline fun Int.fromHex(): Int = if (this in 48..57) {
        this - 48
    } else {
        this - 87
    }

    // because "normal" CPointer<ByteVar>.toByteArray() functions does not support hex (2 Bytes) bytes
    private fun ByteArray.fromHex(): ByteArray {
        val array = ByteArray((size - 2) / 2)
        var index = 0
        for (i in 2 until size step 2) {
            val first = this[i].toInt().fromHex()
            val second = this[i + 1].toInt().fromHex()
            val octet = first.shl(4).or(second)
            array[index] = octet.toByte()
            index++
        }
        return array
    }

    override fun getDouble(index: Int): Double? = getString(index)?.toDouble()

    override fun getLong(index: Int): Long? = getString(index)?.toLong()

    override fun getString(index: Int): String? {
        val value = result[currentRowIndex, index] ?: return null
        require(value is Parameter.Text) {
            "Column $index of row $currentRowIndex isn't a String value."
        }
        return value.text
    }

    public fun getDate(index: Int): LocalDate? = getString(index)?.toLocalDate()
    public fun getTime(index: Int): LocalTime? = getString(index)?.toLocalTime()
    public fun getLocalTimestamp(index: Int): LocalDateTime? = getString(index)?.replace(" ", "T")?.toLocalDateTime()
    public fun getTimestamp(index: Int): Instant? = getString(index)?.let {
        Instant.parse(it.replace(" ", "T"))
    }

    public fun getInterval(index: Int): DateTimePeriod? = getString(index)?.let { DateTimePeriod.parse(it) }
    public fun getUUID(index: Int): UUID? = getString(index)?.toUUID()
}
