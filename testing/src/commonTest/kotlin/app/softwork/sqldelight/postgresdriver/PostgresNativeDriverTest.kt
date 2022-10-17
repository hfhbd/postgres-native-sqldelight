package app.softwork.sqldelight.postgresdriver

import kotlinx.datetime.*
import kotlinx.uuid.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class PostgresNativeDriverTest {
    private val driver = PostgresNativeDriver(
        host = "localhost",
        port = 5432,
        user = "postgres",
        database = "postgres",
        password = "password"
    )

    @Test
    fun allTypes() {
        val queries = NativePostgres(driver).fooQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        assertEquals(emptyList(), queries.get().executeAsList())

        val foo = Foo(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = 42.seconds,
            uuid = UUID.NIL
        )
        queries.create(
            a = foo.a,
            b = foo.b,
            date = foo.date,
            time = foo.time,
            timestamp = foo.timestamp,
            instant = foo.instant,
            interval = foo.interval,
            uuid = foo.uuid
        )
        assertEquals(foo, queries.get().executeAsOne())
    }

    @Test
    fun copyTest() {
        val queries = NativePostgres(driver).fooQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        queries.startCopy()
        val result =
            driver.copy("42,answer,2020-12-12,12:42:00.0000,2014-08-01T12:01:02.0000,1970-01-01T00:00:00.010Z,PT42S,00000000-0000-0000-0000-000000000000")
        assertEquals(1, result)
        val foo = Foo(
            a = 42,
            b = "answer",
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = LocalTime(12, 42, 0, 0),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
            instant = Instant.fromEpochMilliseconds(10L),
            interval = 42.seconds,
            uuid = UUID.NIL,
        )
        assertEquals(foo, queries.get().executeAsOne())
    }

    @Test
    fun userTest() {
        val queries = NativePostgres(driver).usersQueries
        NativePostgres.Schema.migrate(driver, 0, NativePostgres.Schema.version)
        queries.insert("test@test", "test", "bio", "")
        val testUser = queries.selectByUsername("test").executeAsOne()
        assertEquals(
            SelectByUsername(
                "test@test",
                "test",
                "bio",
                ""
            ),
            testUser
        )
    }
}
