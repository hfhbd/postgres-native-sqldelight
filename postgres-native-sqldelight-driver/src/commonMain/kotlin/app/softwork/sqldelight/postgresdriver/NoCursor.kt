package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.Closeable
import app.cash.sqldelight.db.QueryResult

internal class NoCursor(
    result: PGResult
) : PostgresCursor(result), Closeable {
    override fun close() {
        result.clear()
    }

    private val maxRowIndex = result.rows
    override var currentRowIndex = -1

    override fun next(): QueryResult.Value<Boolean> = QueryResult.Value(
        if (currentRowIndex < maxRowIndex) {
            currentRowIndex += 1
            true
        } else {
            false
        }
    )
}
