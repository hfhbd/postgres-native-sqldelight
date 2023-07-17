package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*
import kotlinx.cinterop.*
import libpq.*

/**
 * Must be used inside a transaction!
 */
@ExperimentalForeignApi
internal class RealCursor(
    result: CPointer<PGresult>,
    private val name: String,
    private val conn: CPointer<PGconn>,
    private val fetchSize: Int
) : PostgresCursor(result), Closeable {
    override fun close() {
        result.clear()
        conn.exec("CLOSE $name")
        conn.exec("END")
    }

    override var currentRowIndex = -1
    private var maxRowIndex = -1

    override fun next(): QueryResult.Value<Boolean> {
        if (currentRowIndex == maxRowIndex) {
            currentRowIndex = -1
        }
        if (currentRowIndex == -1) {
            result = PQexec(conn, "FETCH $fetchSize IN $name").check(conn)
            maxRowIndex = PQntuples(result) - 1
        }
        return if (currentRowIndex < maxRowIndex) {
            currentRowIndex += 1
            QueryResult.Value(true)
        } else QueryResult.Value(false)
    }
}
