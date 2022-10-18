package app.softwork.sqldelight.postgresdriver

import kotlinx.cinterop.*
import libpq.*

internal class NoCursor(
    result: CPointer<PGresult>
) : PostgresCursor(result) {
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
