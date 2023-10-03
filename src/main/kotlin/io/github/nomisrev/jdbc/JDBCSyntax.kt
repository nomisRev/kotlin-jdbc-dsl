package io.github.nomisrev.jdbc

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types
import javax.sql.DataSource

/**
 * A small DSL that allows more conveniently directly working with JDBC.
 */
public inline fun <A> DataSource.connection(block: JDBCSyntax.() -> A): A =
  connection.use { conn ->
    block(JDBCSyntax(conn))
  }

@JvmInline
public value class JDBCSyntax(private val conn: Connection) : Connection by conn {

  private fun prepareStatement(
    sql: String,
    binders: (SqlPreparedStatement.() -> Unit)? = null
  ): PreparedStatement =
    prepareStatement(sql)
      .apply { if (binders != null) SqlPreparedStatement(this).binders() }

  /**
   * Update database, this is needed for `INSERT`, `DELETE`, `CREATE TABLE/EXTENSION`, etc.
   * All SQL commands that **do not return any values**.
   */
  public fun update(
    sql: String,
    binders: (SqlPreparedStatement.() -> Unit)? = null,
  ): Int = prepareStatement(sql, binders).use { statement ->
    statement.executeUpdate()
  }

  /**
   * Query database, this is needed for all `SELECT`.
   * All SQL commands that **do return values**.
   */
  public fun <A> queryOneOrNull(
    sql: String,
    binders: (SqlPreparedStatement.() -> Unit)? = null,
    mapper: SqlCursor.() -> A
  ): A? =
    prepareStatement(sql, binders).use { statement ->
      statement.executeQuery().use { resultSet ->
        if (resultSet.next()) mapper(SqlCursor(resultSet)) else null
      }
    }

  /**
   * Query database, this is needed for all `SELECT`.
   * All SQL commands that **do return values**.
   */
  public fun <A> queryAsList(
    sql: String,
    binders: (SqlPreparedStatement.() -> Unit)? = null,
    mapper: SqlCursor.() -> A?
  ): List<A> = prepareStatement(sql, binders).use { statement ->
    statement.executeQuery().use { resultSet ->
      buildList {
        while (resultSet.next()) {
          mapper(SqlCursor(resultSet))?.let(this::add)
        }
      }
    }
  }

  public fun <A> executeBatch(
    sql: String,
    values: Iterable<A>,
    returnGeneratedKeys: Boolean = true,
    binders: (SqlPreparedStatement.(value: A) -> Unit)
  ): List<Long> =
    (if (returnGeneratedKeys) prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    else prepareStatement(sql)).apply {
      values.forEach { a ->
        SqlPreparedStatement(this).binders(a)
        addBatch()
      }
    }.use { statement ->
      val ints = statement.executeBatch()
      if (returnGeneratedKeys) {
        statement.generatedKeys.use { resultSet ->
          buildList {
            while (resultSet.next()) {
              add(resultSet.getLong(1))
            }
          }
        }
      } else ints.map { it.toLong() }
    }


  /** Small DSL Syntax that allows conveniently binding values to `?` in [PreparedStatement]. */
  public class SqlPreparedStatement(private val preparedStatement: PreparedStatement) :
    PreparedStatement by preparedStatement {
    private var index: Int = 1

    public fun bind(short: Short?): Unit = bind(short?.toLong())
    public fun bind(byte: Byte?): Unit = bind(byte?.toLong())
    public fun bind(int: Int?): Unit = bind(int?.toLong())
    public fun bind(char: Char?): Unit = bind(char?.toString())

    public fun bind(bytes: ByteArray?): Unit =
      if (bytes == null) preparedStatement.setNull(index++, Types.BLOB)
      else preparedStatement.setBytes(index++, bytes)

    public fun bind(long: Long?): Unit =
      if (long == null) preparedStatement.setNull(index++, Types.INTEGER)
      else preparedStatement.setLong(index++, long)

    public fun bind(double: Double?): Unit =
      if (double == null) preparedStatement.setNull(index++, Types.REAL)
      else preparedStatement.setDouble(index++, double)

    public fun bind(string: String?): Unit =
      if (string == null) preparedStatement.setNull(index++, Types.VARCHAR)
      else preparedStatement.setString(index++, string)
  }

  /** Small DSL Syntax that allows conveniently extracting values from [ResultSet]. */
  @Suppress("TooManyFunctions")
  public class SqlCursor(private val resultSet: ResultSet) : ResultSet by resultSet {
    private var index: Int = 1

    private fun message(type: String): String =
      "Expected non-null $type at index ${index - 1} but was null."

    public fun int(): Int = requireNotNull(intOrNull()) { message("Int") }
    public fun string(): String = requireNotNull(stringOrNull()) { message("String") }
    public fun bytes(): ByteArray = requireNotNull(bytesOrNull()) { message("ByteArray") }
    public fun long(): Long = requireNotNull(longOrNull()) { message("Long") }
    public fun double(): Double = requireNotNull(doubleOrNull()) { message("Double") }

    public fun intOrNull(): Int? = longOrNull()?.toInt()
    public fun stringOrNull(): String? = resultSet.getString(index++)
    public fun bytesOrNull(): ByteArray? = resultSet.getBytes(index++)
    public fun longOrNull(): Long? = resultSet.getLong(index++).takeUnless { resultSet.wasNull() }
    public fun doubleOrNull(): Double? = resultSet.getDouble(index++).takeUnless { resultSet.wasNull() }
  }
}
