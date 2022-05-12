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
        assertEquals(1, driver.execute(null, "SELECT * from data limit 1;", parameters = 0))
        val s = driver.executeQuery(
            null,
            sql = "SELECT * from data;",
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
