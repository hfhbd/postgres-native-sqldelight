@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.time.*

public expect class PostgresJvmNixPreparedStatement : SqlPreparedStatement

public expect fun PostgresJvmNixPreparedStatement.bindDate(index: Int, value: LocalDate?)
public expect fun PostgresJvmNixPreparedStatement.bindTime(index: Int, value: LocalTime?)
public expect fun PostgresJvmNixPreparedStatement.bindLocalTimestamp(index: Int, value: LocalDateTime?)
public expect fun PostgresJvmNixPreparedStatement.bindTimestamp(index: Int, value: Instant?)

public expect fun PostgresJvmNixPreparedStatement.bindInterval(index: Int, value: DateTimePeriod?)
public expect fun PostgresJvmNixPreparedStatement.bindUUID(index: Int, value: UUID?)
