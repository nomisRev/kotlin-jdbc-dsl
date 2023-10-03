package io.github.nomisrev.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@Testcontainers
class JDBCSyntaxSpec {

  @AfterEach
  fun teardown() {
    dataSource.connection { truncateTable(TABLE_NAME) }
  }

  @Test
  fun queryOneOrNull() = dataSource.connection {
    val id = insertValue("name", null, 30)
    assertEquals(1, id)
  }

  @Test
  fun executeBatch() = dataSource.connection {
    val users = (1..10).map { Triple("name$it", it, null) }
    val ids = insertAll(users)
    assertEquals((1..10L).toList(), ids.toList())
  }

  @Test
  fun selectAll() = dataSource.connection {
    val users = (1..10).map { Triple("name$it", it, null) }
    insertAll(users)
    val actual = selectAll()
    val expected = users.mapIndexed { index, (name, age, address) -> User(index + 1L, name, age, address) }
    assertEquals(expected, actual)
  }

  companion object {
    @Container
    @JvmStatic
    val postgres = PostgreSQLContainer("postgres:latest")

    private val dataSource by lazy {
      HikariDataSource(HikariConfig().apply {
        jdbcUrl = postgres.jdbcUrl
        username = postgres.username
        password = postgres.password
        driverClassName = postgres.driverClassName
      })
    }

    @BeforeAll
    @JvmStatic
    fun setup() {
      dataSource.connection { createTable() }
    }

    @AfterAll
    @JvmStatic
    fun destroy() {
      dataSource.close()
    }
  }
}

private val TABLE_NAME = "testTable"

data class User(val id: Long, val name: String, val age: Int, val address: String?)

fun JDBCSyntax.createTable() =
  update(
    """
      CREATE TABLE $TABLE_NAME(
        id SERIAL PRIMARY KEY,
        name VARCHAR(20) NOT NULL,
        address TEXT,
        age INT NOT NULL
      );
    """.trimIndent()
  )

fun JDBCSyntax.insertValue(name: String, address: String?, age: Int): Long? =
  queryOneOrNull("""
      INSERT INTO $TABLE_NAME(name, address, age)
      VALUES(?, ?, ?)
      RETURNING id;
    """.trimIndent(), {
    bind(name)
    bind(address)
    bind(age)
  }) {
    long()
  }

fun JDBCSyntax.insertAll(users: Iterable<Triple<String, Int, String?>>): List<Long> =
  executeBatch(
    """
      INSERT INTO $TABLE_NAME(name, address, age)
      VALUES(?, ?, ?);
    """.trimIndent(),
    users
  ) { (name, age, address) ->
    bind(name)
    bind(address)
    bind(age)
  }

fun JDBCSyntax.selectAll(): List<User> =
  queryAsList(
    """
      SELECT id, name, address, age
      FROM $TABLE_NAME;
    """.trimIndent()
  ) {
    User(long(), string(), address = stringOrNull(), age = int())
  }

/** Utility function to clear all rows for a certain table */
fun JDBCSyntax.truncateTable(name: String): Int =
  update("TRUNCATE $name RESTART IDENTITY;")

/** Utility function to clear all rows for a set of tables */
fun JDBCSyntax.truncateTables(vararg names: String): Int =
  update("TRUNCATE ${names.joinToString()} RESTART IDENTITY;")
