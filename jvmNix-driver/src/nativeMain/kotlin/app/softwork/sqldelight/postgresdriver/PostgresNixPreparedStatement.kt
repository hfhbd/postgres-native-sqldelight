@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package app.softwork.sqldelight.postgresdriver

import app.softwork.sqldelight.postgresdriver.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.time.*

public actual typealias PostgresJvmNixPreparedStatement = PostgresPreparedStatement

public actual fun PostgresJvmNixPreparedStatement.bindDate(index: Int, value: LocalDate?) {
    error("Extension is shadowed by a member")
}

public actual fun PostgresJvmNixPreparedStatement.bindTime(index: Int, value: LocalTime?) {
    error("Extension is shadowed by a member")
}

public actual fun PostgresJvmNixPreparedStatement.bindLocalTimestamp(index: Int, value: LocalDateTime?) {
    error("Extension is shadowed by a member")
}

public actual fun PostgresJvmNixPreparedStatement.bindTimestamp(index: Int, value: Instant?) {
    error("Extension is shadowed by a member")
}

public actual fun PostgresJvmNixPreparedStatement.bindInterval(index: Int, value: DateTimePeriod?) {
    error("Extension is shadowed by a member")
}

public actual fun PostgresJvmNixPreparedStatement.bindUUID(index: Int, value: UUID?) {
    error("Extension is shadowed by a member")
}
