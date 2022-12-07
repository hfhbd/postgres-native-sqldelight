package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*
import kotlinx.cinterop.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import libpq.*
import kotlin.time.*

public sealed class PostgresCursor(
    internal var result: CPointer<PGresult>
) : SqlCursor {
    internal abstract val currentRowIndex: Int

    override fun getBoolean(index: Int): Boolean? = getString(index)?.toBoolean()

    override fun getBytes(index: Int): ByteArray? {
        val isNull = PQgetisnull(result, tup_num = currentRowIndex, field_num = index) == 1
        return if (isNull) {
            null
        } else {
            val bytes = PQgetvalue(result, tup_num = currentRowIndex, field_num = index)!!
            val length = PQgetlength(result, tup_num = currentRowIndex, field_num = index)
            bytes.fromHex(length)
        }
    }

    private inline fun Int.fromHex(): Int = if (this in 48..57) {
        this - 48
    } else {
        this - 87
    }

    // because "normal" CPointer<ByteVar>.toByteArray() functions does not support hex (2 Bytes) bytes
    private fun CPointer<ByteVar>.fromHex(length: Int): ByteArray {
        val array = ByteArray((length - 2) / 2)
        var index = 0
        for (i in 2 until length step 2) {
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
        val isNull = PQgetisnull(result, tup_num = currentRowIndex, field_num = index) == 1
        return if (isNull) {
            null
        } else {
            val value = PQgetvalue(result, tup_num = currentRowIndex, field_num = index)
            value!!.toKString()
        }
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
