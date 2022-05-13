package app.softwork.sqldelight.postgresdriver

import kotlin.test.*

class PostgresNativeDriverTest {
    @Test
    fun simpleTest() {
        val driver = PostgresNativeDriver(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "postgres",
            password = "password"
        )
        driver.execute(null, "DROP TABLE IF EXISTS foo;", parameters = 0)
        driver.execute(null, "CREATE TABLE foo(a int primary key, bar text);", parameters = 0)
        repeat(5) {
            driver.execute(null, "INSERT INTO foo VALUES ($it, 'a')", parameters = 0)
        }
        repeat(5) {
            driver.execute(null, "INSERT INTO foo VALUES ($1, $2)", parameters = 2) {
                bindLong(0, 5 + it.toLong())
                bindString(1, "bar $it")
            }
        }

        assertEquals(1, driver.execute(null, "SELECT * from foo limit 1;", parameters = 0))
        val s = driver.executeQuery(
            null,
            sql = "SELECT * FROM foo;",
            parameters = 0, binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            S(
                                index = it.getLong(0)!!,
                                name = it.getString(1)!!
                            )
                        )
                    }
                }
            })
        assertEquals(10, s.size)
    }

    data class S(val index: Long, val name: String)
}
