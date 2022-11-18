package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*
import kotlinx.cinterop.*
import libpq.*

internal class NoCursor(
    result: CPointer<PGresult>
) : PostgresCursor(result), Closeable {
    override fun close() {
        result.clear()
    }

    private val maxRowIndex = PQntuples(result) - 1
    override var currentRowIndex = -1

    override fun next(): Boolean {
        return if (currentRowIndex < maxRowIndex) {
            currentRowIndex += 1
            true
        } else {
            false
        }
    }
}
