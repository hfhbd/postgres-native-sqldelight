package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*
import kotlinx.cinterop.*
import kotlinx.datetime.*
import kotlinx.uuid.*

public class PostgresPreparedStatement internal constructor(private val parameters: Int) : SqlPreparedStatement {
    @ExperimentalForeignApi
    internal fun values(scope: AutofreeScope): CValuesRef<CPointerVar<ByteVar>> = createValues(parameters) {
        value = when (val value = _values[it]) {
            null -> null
            is Data.Bytes -> value.bytes.refTo(0).getPointer(scope)
            is Data.Text -> value.text.cstr.getPointer(scope)
        }
    }

    private sealed interface Data {
        value class Bytes(val bytes: ByteArray) : Data
        value class Text(val text: String) : Data
    }

    private val _values = arrayOfNulls<Data>(parameters)
    internal val lengths = IntArray(parameters)
    internal val formats = IntArray(parameters)
    internal val types = UIntArray(parameters)

    private fun bind(index: Int, value: String?, oid: UInt) {
        lengths[index] = if (value != null) {
            _values[index] = Data.Text(value)
            value.length
        } else 0
        formats[index] = PostgresNativeDriver.TEXT_RESULT_FORMAT
        types[index] = oid
    }

    override fun bindBoolean(index: Int, boolean: Boolean?) {
        bind(index, boolean?.toString(), boolOid)
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        lengths[index] = if (bytes != null && bytes.isNotEmpty()) {
            _values[index] = Data.Bytes(bytes)
            bytes.size
        } else 0
        formats[index] = PostgresNativeDriver.BINARY_RESULT_FORMAT
        types[index] = byteaOid
    }

    override fun bindDouble(index: Int, double: Double?) {
        bind(index, double?.toString(), doubleOid)
    }

    override fun bindLong(index: Int, long: Long?) {
        bind(index, long?.toString(), longOid)
    }

    override fun bindString(index: Int, string: String?) {
        bind(index, string, textOid)
    }

    public fun bindDate(index: Int, value: LocalDate?) {
        bind(index, value?.toString(), dateOid)
    }


    public fun bindTime(index: Int, value: LocalTime?) {
        bind(index, value?.toString(), timeOid)
    }

    public fun bindLocalTimestamp(index: Int, value: LocalDateTime?) {
        bind(index, value?.toString(), timestampOid)
    }

    public fun bindTimestamp(index: Int, value: Instant?) {
        bind(index, value?.toString(), timestampTzOid)
    }

    public fun bindInterval(index: Int, value: DateTimePeriod?) {
        bind(index, value?.toString(), intervalOid)
    }

    public fun bindUUID(index: Int, value: UUID?) {
        bind(index, value?.toString(), uuidOid)
    }

    private companion object {
        // Hardcoded, because not provided in libpq-fe.h for unknown reasons...
        // select * from pg_type;
        private const val boolOid = 16u
        private const val byteaOid = 17u
        private const val longOid = 20u
        private const val textOid = 25u
        private const val doubleOid = 701u

        private const val dateOid = 1082u
        private const val timeOid = 1083u
        private const val intervalOid = 1186u
        private const val timestampOid = 1114u
        private const val timestampTzOid = 1184u
        private const val uuidOid = 2950u
    }
}
