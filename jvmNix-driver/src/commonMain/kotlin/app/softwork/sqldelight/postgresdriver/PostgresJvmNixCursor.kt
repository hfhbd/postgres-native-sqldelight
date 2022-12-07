@file:Suppress("NO_ACTUAL_FOR_EXPECT")
package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.time.*

public expect class PostgresJvmNixCursor : SqlCursor

public expect fun PostgresJvmNixCursor.getDate(index: Int): LocalDate?
public expect fun PostgresJvmNixCursor.getTime(index: Int): LocalTime?
public expect fun PostgresJvmNixCursor.getLocalTimestamp(index: Int): LocalDateTime?
public expect fun PostgresJvmNixCursor.getTimestamp(index: Int): Instant?

public expect fun PostgresJvmNixCursor.getInterval(index: Int): DateTimePeriod?
public expect fun PostgresJvmNixCursor.getUUID(index: Int): UUID?
