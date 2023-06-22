package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.datetime.*
import kotlinx.uuid.UUID

public class PostgresPreparedStatement
internal constructor(parameters: Int) : SqlPreparedStatement {
    internal val values: Array<Parameter?> = arrayOfNulls(parameters)

    private fun bind(index: Int, value: String?, oid: PGParamType) {
        values[index] = Parameter.Text(
            text = value,
            type = oid
        )
    }

    override fun bindBoolean(index: Int, boolean: Boolean?) {
        bind(index, boolean?.toString(), PGParamType.BoolOid)
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        values[index] = Parameter.Bytes(
            if (bytes != null && bytes.isNotEmpty()) bytes else byteArrayOf()
        )
    }

    override fun bindDouble(index: Int, double: Double?) {
        bind(index, double?.toString(), PGParamType.DoubleOid)
    }

    override fun bindLong(index: Int, long: Long?) {
        bind(index, long?.toString(), PGParamType.LongOid)
    }

    override fun bindString(index: Int, string: String?) {
        bind(index, string, PGParamType.TextOid)
    }

    public fun bindDate(index: Int, value: LocalDate?) {
        bind(index, value?.toString(), PGParamType.DateOid)
    }


    public fun bindTime(index: Int, value: LocalTime?) {
        bind(index, value?.toString(), PGParamType.TimeOid)
    }

    public fun bindLocalTimestamp(index: Int, value: LocalDateTime?) {
        bind(index, value?.toString(), PGParamType.TimestampOid)
    }

    public fun bindTimestamp(index: Int, value: Instant?) {
        bind(index, value?.toString(), PGParamType.TimestampTzOid)
    }

    public fun bindInterval(index: Int, value: DateTimePeriod?) {
        bind(index, value?.toString(), PGParamType.IntervalOid)
    }

    public fun bindUUID(index: Int, value: UUID?) {
        bind(index, value?.toString(), PGParamType.UuidOid)
    }
}
