package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.*
import app.cash.sqldelight.db.*
import app.softwork.sqldelight.postgresdriver.PostgresNativeDriver.Companion.error
import kotlinx.cinterop.*
import libpq.*

class PostgresNativeDriver(private var conn: CPointer<PGconn>) : SqlDriver {

    private var transaction: Transacter.Transaction? = null

    init {
        require(PQstatus(conn) == ConnStatusType.CONNECTION_OK) {
            conn.error()
        }
        /* Set always-secure search path, so malicious users can't take control. */
        conn.exec("SELECT pg_catalog.set_config('search_path', '', false)")
    }

    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
        TODO("Not yet implemented")
    }

    override fun notifyListeners(queryKeys: Array<String>) {
        TODO("Not yet implemented")
    }

    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
        TODO("Not yet implemented")
    }

    override fun currentTransaction() = transaction

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): Long {
        val preparedStatement = if (parameters != 0) Prepared(parameters).apply {
            if (binders != null) {
                binders()
            }
        } else null
        val result = if (identifier != null) {
            PQprepare(
                conn,
                stmtName = identifier.toString(),
                query = sql,
                nParams = parameters,
                paramTypes = preparedStatement?.types?.refTo(0)
            ).check().clear()

            memScoped {
                PQexecPrepared(
                    conn,
                    stmtName = identifier.toString(),
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    resultFormat = TEXT_RESULT_FORMAT
                )
            }
        } else {
            memScoped {
                PQexecParams(
                    conn,
                    command = sql,
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    resultFormat = TEXT_RESULT_FORMAT,
                    paramTypes = preparedStatement?.types?.refTo(0)
                )
            }
        }.check()
        val rows = PQcmdTuples(result)!!.toKString().toLong()
        result.clear()
        return rows
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> R,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): R {
        val preparedStatement = if (parameters != 0) {
            Prepared(parameters).apply {
                if (binders != null) {
                    binders()
                }
            }
        } else null
        val result = if (identifier != null) {
            require(
                PQsendPrepare(
                    conn,
                    stmtName = identifier.toString(), query = sql, nParams = parameters,
                    paramTypes = preparedStatement?.types?.refTo(0)
                ) == 1
            ) {
                conn.error()
            }
            memScoped {
                PQsendQueryPrepared(
                    conn,
                    stmtName = identifier.toString(),
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    resultFormat = TEXT_RESULT_FORMAT
                )
            }
        } else {
            memScoped {
                PQsendQueryParams(
                    conn,
                    command = sql,
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    paramTypes = preparedStatement?.types?.refTo(0),
                    resultFormat = TEXT_RESULT_FORMAT
                )
            }
        }
        require(result == 1) { conn.error() }
        require(PQsetSingleRowMode(conn) == 1) { conn.error() }
        return Cursor().use(mapper)
    }

    internal companion object {
        private const val TEXT_RESULT_FORMAT = 0
        private const val BINARY_RESULT_FORMAT = 1

        // Hardcoded, because not provided in libpq-fe.h for unknown reasons...
        private const val boolOid = 16u
        private const val byteaOid = 17u
        private const val longOid = 20u
        private const val textOid = 25u
        private const val doubleOid = 701u

        internal fun CPointer<PGconn>?.error(): String {
            val errorMessage = PQerrorMessage(this)!!.toKString()
            PQfinish(this)
            return errorMessage
        }

        private fun CPointer<PGresult>?.clear() {
            PQclear(this)
        }
    }

    private fun CPointer<PGconn>.exec(sql: String) {
        PQexec(this, sql).check().clear()
    }

    private fun CPointer<PGresult>?.check(): CPointer<PGresult> {
        require(PQresultStatus(this) == PGRES_TUPLES_OK) {
            conn.error()
        }
        return this!!
    }

    private inner class Cursor : SqlCursor, Closeable {
        private var currentRow = -1 // rows start at 0, and next is called at start
        private var result: CPointer<PGresult>? = null

        override fun close() {
            result?.clear()
        }

        override fun getBoolean(index: Int) = getString(index)?.toBoolean()

        override fun getBytes(index: Int) = getString(index)?.encodeToByteArray()

        override fun getDouble(index: Int) = getString(index)?.toDouble()

        override fun getLong(index: Int) = getString(index)?.toLong()

        override fun getString(index: Int): String? {
            val isNull = PQgetisnull(result, tup_num = currentRow, field_num = index) == 1
            return if (isNull) {
                null
            } else {
                PQgetvalue(result, tup_num = currentRow, field_num = index)!!.toKString()
            }
        }

        override fun next(): Boolean {
            val next = PQgetResult(conn)
            return if (next != null) {
                result.clear()
                result = next
                currentRow++
                true
            } else false
        }
    }

    private class Prepared(private val parameters: Int) : SqlPreparedStatement {
        fun values(scope: AutofreeScope): CValuesRef<CPointerVar<ByteVar>> = createValues(parameters) {
            value = _values[it]?.cstr?.getPointer(scope)
        }

        private val _values = arrayOfNulls<String>(parameters)
        val lengths = IntArray(parameters)
        val formats = IntArray(parameters)
        val types = UIntArray(parameters)

        private fun bind(index: Int, value: String?, oid: UInt) {
            lengths[index] = if (value != null) {
                _values[index] = value
                value.length
            } else 0
            formats[index] = TEXT_RESULT_FORMAT
            types[index] = oid
        }

        override fun bindBoolean(index: Int, boolean: Boolean?) {
            bind(index, boolean?.toString(), boolOid)
        }

        override fun bindBytes(index: Int, bytes: ByteArray?) {
            bind(index, bytes?.decodeToString(), byteaOid)
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

fun PostgresNativeDriver(
    host: String,
    database: String,
    user: String,
    password: String,
    port: Int? = null,
    options: String? = null
): PostgresNativeDriver {
    val conn = PQsetdbLogin(
        pghost = host,
        pgport = port?.toString(),
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
