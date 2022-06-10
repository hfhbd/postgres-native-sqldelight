package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.*
import app.cash.sqldelight.db.*
import kotlinx.cinterop.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import libpq.*
import kotlin.time.*

class PostgresNativeDriver(private var conn: CPointer<PGconn>) : SqlDriver {
    private var transaction: Transacter.Transaction? = null

    init {
        require(PQstatus(conn) == ConnStatusType.CONNECTION_OK) {
            conn.error()
        }
        setDateOutputs()
    }

    private fun setDateOutputs() {
        execute(null, "SET intervalstyle = 'iso_8601';", 0)
        execute(null, "SET datestyle = 'ISO';", 0)
    }

    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {

    }

    override fun notifyListeners(queryKeys: Array<String>) {

    }

    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {

    }

    override fun currentTransaction() = transaction

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult.Value<Long> {
        val preparedStatement = if (parameters != 0) PostgresPreparedStatement(parameters).apply {
            if (binders != null) {
                binders()
            }
        } else null
        val result = if (identifier != null) {
            if (!preparedStatementExists(identifier)) {
                PQprepare(
                    conn,
                    stmtName = identifier.toString(),
                    query = sql.replaceQuestionMarks(),
                    nParams = parameters,
                    paramTypes = preparedStatement?.types?.refTo(0)
                ).check(conn).clear()
            }
            memScoped {
                PQexecPrepared(
                    conn,
                    stmtName = identifier.toString(),
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    resultFormat = BINARY_RESULT_FORMAT
                )
            }
        } else {
            memScoped {
                PQexecParams(
                    conn,
                    command = sql.replaceQuestionMarks(),
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    resultFormat = BINARY_RESULT_FORMAT,
                    paramTypes = preparedStatement?.types?.refTo(0)
                )
            }
        }.check(conn)
        val rows = PQcmdTuples(result)!!.toKString()
        result.clear()
        val resultRows = rows.toLongOrNull() ?: 0
        return QueryResult.Value(value = resultRows)
    }

    private fun preparedStatementExists(identifier: Int): Boolean {
        val result =
            executeQuery(null, "SELECT name FROM pg_prepared_statements WHERE name = $1", parameters = 1, binders = {
                bindString(1, identifier.toString())
            }, mapper = {
                it.next()
                it.getString(0)
            })
        return result.value != null
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> R,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult.Value<R> {
        val cursorName = if (identifier == null) "myCursor" else "cursor$identifier"
        val cursor = "DECLARE $cursorName CURSOR FOR"
        val preparedStatement = if (parameters != 0) {
            PostgresPreparedStatement(parameters).apply {
                if (binders != null) {
                    binders()
                }
            }
        } else null
        val result = if (identifier != null) {
            if (!preparedStatementExists(identifier)) {
                PQprepare(
                    conn,
                    stmtName = identifier.toString(),
                    query = "$cursor ${sql.replaceQuestionMarks()}",
                    nParams = parameters,
                    paramTypes = preparedStatement?.types?.refTo(0)
                ).check(conn).clear()
            }
            conn.exec("BEGIN")
            memScoped {
                PQexecPrepared(
                    conn,
                    stmtName = identifier.toString(),
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    resultFormat = BINARY_RESULT_FORMAT
                )
            }
        } else {
            conn.exec("BEGIN")
            memScoped {
                PQexecParams(
                    conn,
                    command = "$cursor ${sql.replaceQuestionMarks()}",
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    paramTypes = preparedStatement?.types?.refTo(0),
                    resultFormat = BINARY_RESULT_FORMAT
                )
            }
        }.check(conn)

        val value = PostgresCursor(result, cursorName, conn).use(mapper)
        return QueryResult.Value(value = value)
    }

    private fun String.replaceQuestionMarks(): String {
        var index = 1
        return replace(replaceQuestionMarks) {
            "$${index++}"
        }
    }

    internal companion object {
        const val TEXT_RESULT_FORMAT = 0
        const val BINARY_RESULT_FORMAT = 1
        val replaceQuestionMarks = "\\?".toRegex()
    }

    override fun close() {
        PQfinish(conn)
    }

    override fun newTransaction(): Transacter.Transaction {
        conn.exec("BEGIN")
        return Transaction(transaction)
    }

    private inner class Transaction(
        override val enclosingTransaction: Transacter.Transaction?
    ) : Transacter.Transaction() {
        override fun endTransaction(successful: Boolean) {
            if (enclosingTransaction == null) {
                if (successful) {
                    conn.exec("END")
                } else {
                    conn.exec("ROLLBACK")
                }
            }
            transaction = enclosingTransaction
        }
    }
}

private fun CPointer<PGconn>?.error(): String {
    val errorMessage = PQerrorMessage(this)!!.toKString()
    PQfinish(this)
    return errorMessage
}

private fun CPointer<PGresult>?.clear() {
    PQclear(this)
}

private fun CPointer<PGconn>.exec(sql: String) {
    val result = PQexec(this, sql)
    result.check(this)
    result.clear()
}

private fun CPointer<PGresult>?.check(conn: CPointer<PGconn>): CPointer<PGresult> {
    val status = PQresultStatus(this)
    check(status == PGRES_TUPLES_OK || status == PGRES_COMMAND_OK) {
        conn.error()
    }
    return this!!
}

/**
 * Must be inside a transaction!
 */
class PostgresCursor(
    private var result: CPointer<PGresult>,
    private val name: String,
    private val conn: CPointer<PGconn>
) : SqlCursor, Closeable {
    override fun close() {
        result.clear()
        conn.exec("CLOSE $name")
        conn.exec("END")
    }

    override fun getBoolean(index: Int) = getString(index)?.toBoolean()

    override fun getBytes(index: Int): ByteArray? {
        val isNull = PQgetisnull(result, tup_num = 0, field_num = index) == 1
        return if (isNull) {
            null
        } else {
            val bytes = PQgetvalue(result, tup_num = 0, field_num = index)!!
            val length = PQgetlength(result, tup_num = 0, field_num = index)
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

    override fun getDouble(index: Int) = getString(index)?.toDouble()

    override fun getLong(index: Int) = getString(index)?.toLong()

    override fun getString(index: Int): String? {
        val isNull = PQgetisnull(result, tup_num = 0, field_num = index) == 1
        return if (isNull) {
            null
        } else {
            val value = PQgetvalue(result, tup_num = 0, field_num = index)
            value!!.toKString()
        }
    }

    fun getDate(index: Int): LocalDate? = getString(index)?.toLocalDate()

    // fun getTime(index: Int): LocalTime? = getInt(index)?.toLocalTime()
    fun getLocalTimestamp(index: Int): LocalDateTime? = getString(index)?.replace(" ", "T")?.toLocalDateTime()
    fun getTimestamp(index: Int): Instant? = getString(index)?.let {
        Instant.parse(it.replace(" ", "T"))
    }

    fun getInterval(index: Int): Duration? = getString(index)?.let { Duration.parseIsoString(it) }
    fun getUUID(index: Int): UUID? = getString(index)?.toUUID()

    override fun next(): Boolean {
        result = PQexec(conn, "FETCH NEXT IN $name").check(conn)
        return PQcmdTuples(result)!!.toKString().toInt() == 1
    }
}

class PostgresPreparedStatement(private val parameters: Int) : SqlPreparedStatement {
    fun values(scope: AutofreeScope): CValuesRef<CPointerVar<ByteVar>> = createValues(parameters) {
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
    val lengths = IntArray(parameters)
    val formats = IntArray(parameters)
    val types = UIntArray(parameters)

    private fun bind(index: Int, value: String?, oid: UInt) {
        lengths[index - 1] = if (value != null) {
            _values[index - 1] = Data.Text(value)
            value.length
        } else 0
        formats[index - 1] = PostgresNativeDriver.TEXT_RESULT_FORMAT
        types[index - 1] = oid
    }

    override fun bindBoolean(index: Int, boolean: Boolean?) {
        bind(index, boolean?.toString(), boolOid)
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        lengths[index - 1] = if (bytes != null && bytes.isNotEmpty()) {
            _values[index - 1] = Data.Bytes(bytes)
            bytes.size
        } else 0
        formats[index - 1] = PostgresNativeDriver.BINARY_RESULT_FORMAT
        types[index - 1] = byteaOid
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

    fun bindDate(index: Int, value: LocalDate?) {
        bind(index, value?.toString(), dateOid)
    }

    /*
    fun bindTime(index: Int, value: LocalTime?) {
        bind(index, value?.toString(), timeOid)
    }
    */

    fun bindLocalTimestamp(index: Int, value: LocalDateTime?) {
        bind(index, value?.toString(), timestampOid)
    }

    fun bindTimestamp(index: Int, value: Instant?) {
        bind(index, value?.toString(), timestampTzOid)
    }

    fun bindInterval(index: Int, value: Duration?) {
        bind(index, value?.toIsoString(), intervalOid)
    }

    fun bindUUID(index: Int, value: UUID?) {
        bind(index, value?.toString(), uuidOid)
    }

    companion object {
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

fun PostgresNativeDriver(
    host: String, database: String, user: String, password: String, port: Int = 5432, options: String? = null
): PostgresNativeDriver {
    val conn = PQsetdbLogin(
        pghost = host,
        pgport = port.toString(),
        pgtty = null,
        dbName = database,
        login = user,
        pwd = password,
        pgoptions = options
    )
    require(PQstatus(conn) == ConnStatusType.CONNECTION_OK) {
        conn.error()
    }
    return PostgresNativeDriver(conn!!)
}
