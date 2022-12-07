@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package app.softwork.sqldelight.postgresdriver

import app.softwork.sqldelight.postgresdriver.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.time.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
public actual typealias PostgresJvmNixCursor = PostgresCursor

public actual fun PostgresJvmNixCursor.getDate(index: Int): LocalDate? = error("Extension is shadowed by a member")
public actual fun PostgresJvmNixCursor.getTime(index: Int): LocalTime? = error("Extension is shadowed by a member")
public actual fun PostgresJvmNixCursor.getLocalTimestamp(index: Int): LocalDateTime? =
    error("Extension is shadowed by a member")

public actual fun PostgresJvmNixCursor.getTimestamp(index: Int): Instant? = error("Extension is shadowed by a member")

public actual fun PostgresJvmNixCursor.getInterval(index: Int): DateTimePeriod? = error("Extension is shadowed by a member")
public actual fun PostgresJvmNixCursor.getUUID(index: Int): UUID? = error("Extension is shadowed by a member")
