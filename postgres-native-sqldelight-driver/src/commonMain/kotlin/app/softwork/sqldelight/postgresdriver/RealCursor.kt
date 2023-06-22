package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*

/**
 * Must be used inside a transaction.
 */
internal class RealCursor(
    result: PGResult,
    private val name: String,
    private val conn: PGConnection,
    private val fetchSize: Int
) : PostgresCursor(result), Closeable {
    override fun close() {
        result.clear()
        conn.execParams("CLOSE $name")
        conn.execParams("END")
    }

    override var currentRowIndex = -1
    private var maxRowIndex = -1

    override fun next(): QueryResult.Value<Boolean> {
        if (currentRowIndex == maxRowIndex) {
            currentRowIndex = -1
        }
        if (currentRowIndex == -1) {
            result = conn.execParams("FETCH $fetchSize IN $name", emptyList(), Format.Text).check(conn)
            maxRowIndex = result.rows.toInt() - 1
        }
        return if (currentRowIndex < maxRowIndex) {
            currentRowIndex += 1
            QueryResult.Value(true)
        } else QueryResult.Value(false)
    }
}
