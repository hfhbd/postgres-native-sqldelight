package app.softwork.sqldelight.postgresdriver

import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class PostgresNativeDriverTest {
    @Test
    fun allTypes() {
        val driver = PostgresNativeDriver(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "postgres",
            password = "password"
        )
        val queries = NativePostgres(driver).fooQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        assertEquals(emptyList(), queries.get().executeAsList())

        val foo = Foo(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            uuid = UUID.NIL,
            interval = 42.seconds
        )
        queries.create(foo)
        assertEquals(foo, queries.get().executeAsOne())
    }
}
