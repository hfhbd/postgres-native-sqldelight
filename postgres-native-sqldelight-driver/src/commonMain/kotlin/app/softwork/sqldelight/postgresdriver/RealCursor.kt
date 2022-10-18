package app.softwork.sqldelight.postgresdriver

import kotlinx.cinterop.*
import libpq.*

/**
 * Must be inside a transaction!
 */
internal class RealCursor(
    result: CPointer<PGresult>,
    private val name: String,
    private val conn: CPointer<PGconn>,
    private val fetchSize: Int
) : PostgresCursor(result) {
    override fun close() {
        result.clear()
        conn.exec("CLOSE $name")
        conn.exec("END")
    }

    override var currentRowIndex = -1
    private var maxRowIndex = -1

    override fun next(): Boolean {
        if (currentRowIndex == maxRowIndex) {
            currentRowIndex = -1
        }
        if (currentRowIndex == -1) {
            result = PQexec(conn, "FETCH $fetchSize IN $name").check(conn)
            maxRowIndex = PQntuples(result) - 1
        }
        return if (currentRowIndex < maxRowIndex) {
            currentRowIndex += 1
            true
        } else false
    }
}
