package app.softwork.sqldelight.postgresdriver

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive

internal class PGConnection(
    val connection: Connection,
) {
    
    val status: PGStatus get() = error("")

    fun error(): String {
        val errorMessage = PQerrorMessage(this)!!.toKString()
        PQfinish(this)
        return errorMessage
    }

    fun prepare(
        stmtName: String,
        query: String,
        parameterTypes: List<PGParamType>
    ): PGResult {

    }

    fun execParams(
        command: String,
        parameters: List<PostgresPreparedStatement.Parameter> = emptyList(),
        resultFormat: Format = Format.Text
    ): PGResult {

    }

    fun execPrepared(
        stmtName: String,
        parameters: List<PostgresPreparedStatement.Parameter>,
        resultFormat: Format
    ): PGResult {

    }

    internal val socket: Int get() = error("")
    
    internal fun notifications(notificationScope: CoroutineScope): SharedFlow<PGNotify> {
        val selector = SelectorManager()

        try {
            val socket = socket
            check(socket >= 0) {
                "Error while connecting to the PostgreSql socket"
            }
            val selectable = object : Selectable {
                override val descriptor: Int = socket
            }

            while (isActive) {
                selector.select(selectable, SelectInterest.READ)
                conn.result
                var notification: PGNotify? = null
                while (PQnotifies(conn)?.pointed?.also { notification = it } != null) {
                    notification?.let {
                        val tableName = it.relname!!.toKString()
                        PQfreemem(it.ptr)
                        send(tableName)
                    }
                }
            }
        } finally {
            selector.close()
        }
    }
}

internal data class PGNotify(val tableName: String)

internal enum class Format {
    Text,
    Binary
}

internal enum class PGParamType(val type: UInt) {
    // Hardcoded, because not provided in libpq-fe.h for unknown reasons...
    // select * from pg_type;
    BoolOid(16u),
    ByteaOid(17u),
    LongOid(20u),
    TextOid(25u),
    DoubleOid(701u),
    DateOid(1082u),
    TimeOid(1083u),
    IntervalOid(1186u),
    TimestampOid(1114u),
    TimestampTzOid(1184u),
    UuidOid(2950u),
}

internal enum class PGStatus {
    CONNECTION_OK,
    PGRES_TUPLES_OK,
    PGRES_COMMAND_OK,
    PGRES_COPY_IN,
}

internal class PGResult {
    val rows: Long get() = TODO()
    val status: PGStatus get() = TODO()
    fun clear() {

    }

    operator fun get(rowIndex: Int, columnIndex: Int): Parameter? {

    }
}

internal sealed interface Parameter {
    val length: Int
    val format: Format
    val type: PGParamType

    class Bytes(
        val bytes: ByteArray?,
    ) : Parameter {
        override val format: Format = Format.Binary
        override val type: PGParamType = PGParamType.ByteaOid
        override val length: Int = bytes?.size ?: 0
    }

    class Text(
        val text: String?,
        override val type: PGParamType
    ) : Parameter {
        override val format: Format = Format.Text
        override val length: Int = text?.length ?: 0
    }
}
