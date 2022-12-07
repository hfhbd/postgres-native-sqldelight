@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.db.*
import app.cash.sqldelight.driver.jdbc.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlinx.uuid.UUID
import org.postgresql.util.*
import java.util.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

public actual typealias PostgresJvmNixPreparedStatement = JdbcPreparedStatement

public actual fun PostgresJvmNixPreparedStatement.bindDate(index: Int, value: LocalDate?) {
    bindObject(index, value?.toJavaLocalDate())
}

public actual fun PostgresJvmNixPreparedStatement.bindTime(index: Int, value: LocalTime?) {
    bindObject(index, value?.toJavaLocalTime())
}

public actual fun PostgresJvmNixPreparedStatement.bindLocalTimestamp(index: Int, value: LocalDateTime?) {
    bindObject(index, value?.toJavaLocalDateTime())
}

public actual fun PostgresJvmNixPreparedStatement.bindTimestamp(index: Int, value: Instant?) {
    bindObject(index, value?.toJavaInstant())
}

public actual fun PostgresJvmNixPreparedStatement.bindInterval(index: Int, value: DateTimePeriod?) {
    val interval = value?.run {
        val seconds = (seconds.seconds + nanoseconds.nanoseconds).toDouble(DurationUnit.SECONDS)
        PGInterval(years, months, days, hours, minutes, seconds)
    }
    bindObject(index, interval)
}

public actual fun PostgresJvmNixPreparedStatement.bindUUID(index: Int, value: UUID?) {
    bindObject(index, value?.toJavaUUID())
}
