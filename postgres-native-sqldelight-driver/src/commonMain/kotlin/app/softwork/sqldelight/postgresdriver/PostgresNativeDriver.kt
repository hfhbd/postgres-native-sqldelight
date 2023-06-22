package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.*
import app.cash.sqldelight.db.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

public class PostgresNativeDriver
internal constructor(
    private val conn: PGConnection,
    private val listenerSupport: ListenerSupport
) : SqlDriver {
    private var transaction: Transacter.Transaction? = null

    private val notifications: Flow<String>

    init {
        require(conn.status == PGStatus.CONNECTION_OK) {
            conn.error()
        }
        setDateOutputs()

        notifications = when (listenerSupport) {
            is ListenerSupport.Local -> listenerSupport.notifications
            is ListenerSupport.Remote -> listenerSupport.remoteListener(conn)
            is ListenerSupport.None -> emptyFlow()
        }
    }

    private fun setDateOutputs() {
        execute(null, "SET intervalstyle = 'iso_8601';", 0)
        execute(null, "SET datestyle = 'ISO';", 0)
    }

    private val listeners = mutableMapOf<Query.Listener, Job>()

    private fun CoroutineScope.listen(queryKeys: List<String>, action: suspend (String) -> Unit) =
        launch {
            notifications.filter {
                it in queryKeys
            }.collect {
                action(it)
            }
        }

    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
        when (listenerSupport) {
            ListenerSupport.None -> return
            is ListenerSupport.Local -> {
                listeners[listener] = listenerSupport.notificationScope.listen(queryKeys.toList()) {
                    listener.queryResultsChanged()
                }
            }

            is ListenerSupport.Remote -> {
                val queryKeysRenamed = queryKeys.map {
                    listenerSupport.notificationName(it)
                }
                listeners[listener] = listenerSupport.notificationScope.listen(queryKeysRenamed) {
                    listener.queryResultsChanged()
                }
                for (queryKey in queryKeysRenamed) {
                    execute(null, "LISTEN ${conn.escaped(queryKey)}", parameters = 0)
                }
            }
        }
    }

    override fun notifyListeners(queryKeys: Array<String>) {
        when (listenerSupport) {
            is ListenerSupport.Local -> {
                listenerSupport.notificationScope.launch {
                    for (queryKey in queryKeys) {
                        listenerSupport.notify(queryKey)
                    }
                }
            }

            is ListenerSupport.Remote -> {
                for (queryKey in queryKeys) {
                    val name = listenerSupport.notificationName(queryKey)
                    execute(null, "NOTIFY ${conn.escaped(name)}", parameters = 0)
                }
            }

            ListenerSupport.None -> return
        }
    }

    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
        val queryListeners = listeners[listener]
        if (queryListeners != null) {
            if (listenerSupport is ListenerSupport.Remote) {
                for (queryKey in queryKeys) {
                    val name = listenerSupport.notificationName(queryKey)
                    execute(null, "UNLISTEN ${conn.escaped(name)}", parameters = 0)
                }
            }
            queryListeners.cancel()
            listeners.remove(listener)
        }
    }

    override fun currentTransaction(): Transacter.Transaction? = transaction

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult.Value<Long> {
        val parameters = if (parameters != 0) PostgresPreparedStatement(parameters).let {
            if (binders != null) {
                it.binders()
            }
            it.values.mapIndexed { index, parameter ->
                requireNotNull(parameter) {
                    "No parameter specified for $index."
                }
            }
        } else emptyList()
        val result = if (identifier != null) {
            if (!preparedStatementExists(identifier)) {
                conn.prepare(
                    stmtName = identifier.toString(),
                    query = sql,
                    parameterTypes = parameters.map { it.type }
                ).check(conn).clear()
            }
            conn.execPrepared(
                stmtName = identifier.toString(),
                parameters = parameters,
                resultFormat = Format.Text
            )
        } else {
            conn.execParams(
                command = sql,
                parameters = parameters,
                resultFormat = Format.Text,
            )
        }.check(conn)

        return QueryResult.Value(value = result.rows)
    }

    private fun preparedStatementExists(identifier: Int): Boolean {
        val result =
            executeQuery(
                null,
                "SELECT name FROM pg_prepared_statements WHERE name = '$identifier'",
                parameters = 0,
                binders = null,
                mapper = {
                    it as PostgresCursor
                    QueryResult.Value(
                        if (it.next().value) {
                            it.getString(0)
                        } else null
                    )
                })
        return result.value != null
    }

    private fun Int.escapeNegative(): String = if (this < 0) "_${toString().substring(1)}" else toString()

    private fun preparedStatement(
        parameters: Int,
        binders: (PostgresPreparedStatement.() -> Unit)?
    ): PostgresPreparedStatement? = if (parameters != 0) {
        PostgresPreparedStatement(parameters).apply {
            if (binders != null) {
                binders()
            }
        }
    } else null

    private fun checkPreparedStatement(
        identifier: Int,
        sql: String,
        preparedStatement: PostgresPreparedStatement?
    ) {
        if (!preparedStatementExists(identifier)) {
            conn.prepare(
                stmtName = identifier.toString(),
                query = sql,
                parameterTypes = preparedStatement?.values?.mapIndexed { index, it ->
                    requireNotNull(it) {
                        "No parameter specified for $index."
                    }.type
                } ?: emptyList(),
            ).check(conn).clear()
        }
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult.Value<R> {
        val preparedStatement = preparedStatement(parameters, binders)
        val result = if (identifier != null) {
            checkPreparedStatement(identifier, sql, parameters, preparedStatement)
            conn.execPrepared(
                stmtName = identifier.toString(),
                nParams = parameters,
                paramValues = preparedStatement?.values,
                paramLengths = preparedStatement?.lengths,
                paramFormats = preparedStatement?.formats,
                resultFormat = Format.Text
            )
        } else {
            conn.execParams(
                command = sql,
                nParams = parameters,
                paramValues = preparedStatement?.values(this),
                paramLengths = preparedStatement?.lengths?.refTo(0),
                paramFormats = preparedStatement?.formats?.refTo(0),
                paramTypes = preparedStatement?.types?.refTo(0),
                resultFormat = TEXT_RESULT_FORMAT
            )
        }.check(conn)

        val value = NoCursor(result).use(mapper)
        return QueryResult.Value(value = value)
    }

    internal companion object {
        const val TEXT_RESULT_FORMAT = 0
        const val BINARY_RESULT_FORMAT = 1
    }

    override fun close() {
        PQfinish(conn)
        if (listenerSupport is ScopedListenerSupport) {
            listenerSupport.notificationScope.cancel()
        }
    }

    override fun newTransaction(): QueryResult.Value<Transacter.Transaction> {
        conn.exec("BEGIN")
        return QueryResult.Value(Transaction(transaction))
    }

    private inner class Transaction(
        override val enclosingTransaction: Transacter.Transaction?
    ) : Transacter.Transaction() {
        override fun endTransaction(successful: Boolean): QueryResult.Unit {
            if (enclosingTransaction == null) {
                if (successful) {
                    conn.exec("END")
                } else {
                    conn.exec("ROLLBACK")
                }
            }
            transaction = enclosingTransaction
            return QueryResult.Unit
        }
    }

    // Custom functions

    /**
     * Each element of stdin can be up to 2 GB.
     */
    public fun copy(stdin: Sequence<String>): Long {
        for (stdin in stdin) {
            val status = PQputCopyData(conn, stdin, stdin.encodeToByteArray().size)
            check(status == 1) {
                conn.error()
            }
        }
        val end = PQputCopyEnd(conn, null)
        check(end == 1) {
            conn.error()
        }
        val result = PQgetResult(conn).check(conn)
        return result.rows
    }

    public fun <R> executeQueryAsFlow(
        identifier: Int?,
        sql: String,
        mapper: suspend (PostgresCursor) -> R,
        parameters: Int,
        fetchSize: Int = 1,
        binders: (PostgresPreparedStatement.() -> Unit)?
    ): Flow<R> = flow {
        val (result, cursorName) = prepareQuery(identifier, sql, parameters, binders)
        RealCursor(result, cursorName, conn, fetchSize).use {
            while (it.next()) {
                emit(mapper(it))
            }
        }
    }

    private fun prepareQuery(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (PostgresPreparedStatement.() -> Unit)?
    ): Pair<CPointer<PGresult>, String> {
        val cursorName = if (identifier == null) "myCursor" else "cursor${identifier.escapeNegative()}"
        val cursor = "DECLARE $cursorName CURSOR FOR"

        val preparedStatement = preparedStatement(parameters, binders)
        return if (identifier != null) {
            checkPreparedStatement(identifier, "$cursor $sql", parameters, preparedStatement)
            conn.exec("BEGIN")
            memScoped {
                PQexecPrepared(
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
            conn.exec("BEGIN")
            memScoped {
                PQexecParams(
                    conn,
                    command = "$cursor $sql",
                    nParams = parameters,
                    paramValues = preparedStatement?.values(this),
                    paramLengths = preparedStatement?.lengths?.refTo(0),
                    paramFormats = preparedStatement?.formats?.refTo(0),
                    paramTypes = preparedStatement?.types?.refTo(0),
                    resultFormat = TEXT_RESULT_FORMAT
                )
            }
        }.check(conn) to cursorName
    }

    @Deprecated(
        "Use executeQueryAsFlow instead or enter your use-case in https://github.com/hfhbd/postgres-native-sqldelight/issues/121",
        replaceWith = ReplaceWith("executeQueryAsFlow(identifier, sql, mapper, parameters, fetchSize, binders)")
    )
    public fun <R> executeQueryWithNativeCursor(
        identifier: Int?,
        sql: String,
        mapper: (PostgresCursor) -> R,
        parameters: Int,
        fetchSize: Int = 1,
        binders: (PostgresPreparedStatement.() -> Unit)?
    ): QueryResult.Value<R> {
        val (result, cursorName) = prepareQuery(identifier, sql, parameters, binders)
        val value = RealCursor(result, cursorName, conn, fetchSize).use(mapper)
        return QueryResult.Value(value = value)
    }
}

internal fun PGResult.check(conn: PGConnection): PGResult {
    val status = status
    check(status == PGStatus.PGRES_TUPLES_OK || status == PGStatus.PGRES_COMMAND_OK || status == PGStatus.PGRES_COPY_IN) {
        conn.error()
    }
    return this
}

private fun PGConnection.escaped(value: String): String {
    val cString = PQescapeIdentifier(this, value, value.length.convert())
    val escaped = cString!!.toKString()
    PQfreemem(cString)
    return escaped
}

public suspend fun PostgresNativeDriver(
    host: String,
    database: String,
    user: String,
    password: String,
    port: Int = 5432,
    options: String? = null,
    listenerSupport: ListenerSupport = ListenerSupport.None
): PostgresNativeDriver {
    val connection = aSocket(
        selector = SelectorManager()
    ).tcp().connect(
        hostname = host,
        port = port
    ) {

    }.connection()

    connection.login(
        database = database,
        username = user,
        password = password,
        options = options
    )

    return PostgresNativeDriver(
        PGConnection(
            connection
        ),
        listenerSupport = listenerSupport
    )
}

private suspend fun Connection.login(
    database: String,
    username: String,
    password: String,
    options: String?
) {
    val size: Int
    val startupMessage = buildPacket {
        writeShort(3) // protocol version 3.0
        writeShort(0)

        writeText("user", charset = Charsets.ISO_8859_1)
        writeText(username)

        writeText("database", charset = Charsets.ISO_8859_1)
        writeText(database)
        size = this.size
    }
    val data = buildPacket {
        writeInt(size + 8)
        writePacket(startupMessage)
    }
    output.writePacket(data)
    input.awaitContent()
    require(input.readByte() == "R".toByte())
    val length = input.readInt()
    when (input.readInt()) {
        3 -> {
            val clearTextPassword = buildPacket {
                val headerSize: Int
                val header = buildPacket {
                    writeText("p")
                    headerSize = this.size
                }
                val passwordSize: Int
                val password = buildPacket {
                    writeText(password)
                    passwordSize = this.size
                }
                writePacket(header)
                writeInt(headerSize + passwordSize + 8)
                writePacket(password)
            }
            output.writePacket(clearTextPassword)
        }
    }
    input.waitForReadyForQuery()
}

private suspend fun ByteReadChannel.waitForReadyForQuery(): ReadyForQuery {
    require(readByte() == "Z".toByte())
    val length = readInt()
    val state = readByte().toString()
    ReadyForQuery(
        transactionState = ReadyForQuery.TransactionStatus.
    )
}

internal data class ReadyForQuery(
    val transactionState: TransactionStatus
) {
    internal enum class TransactionStatus(val type: String) {
        Idle("I"),
        Transaction("T"),
        Error("E");
    }
}
