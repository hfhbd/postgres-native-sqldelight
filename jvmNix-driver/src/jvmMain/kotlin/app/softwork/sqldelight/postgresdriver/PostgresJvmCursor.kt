package app.softwork.sqldelight.postgresdriver

import app.cash.sqldelight.driver.jdbc.*
import kotlinx.datetime.*
import kotlinx.uuid.*
import org.postgresql.util.PGInterval
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

public actual typealias PostgresJvmNixCursor = JdbcCursor

public actual fun PostgresJvmNixCursor.getDate(index: Int): LocalDate? =
    (getObject(index) as java.time.LocalDate?)?.toKotlinLocalDate()

public actual fun PostgresJvmNixCursor.getTime(index: Int): LocalTime? =
    (getObject(index) as java.time.LocalTime?)?.toKotlinLocalTime()

public actual fun PostgresJvmNixCursor.getLocalTimestamp(index: Int): LocalDateTime? =
    (getObject(index) as java.time.LocalDateTime?)?.toKotlinLocalDateTime()

public actual fun PostgresJvmNixCursor.getTimestamp(index: Int): Instant? =
    (getObject(index) as java.time.OffsetDateTime?)?.toInstant()?.toKotlinInstant()

public actual fun JdbcCursor.getInterval(index: Int): DateTimePeriod? {
    return getObject<PGInterval>(index)?.run {
        val seconds = seconds.seconds
        DateTimePeriod(years = years, months = months, days = days, hours = hours, minutes = minutes,
            nanoseconds = seconds.toLong(DurationUnit.NANOSECONDS)
        )
    }
}

public actual fun PostgresJvmNixCursor.getUUID(index: Int): UUID? =
    (getObject(index) as java.util.UUID?)?.toKotlinUUID()
