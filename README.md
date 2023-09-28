# Module Kotlin JDBC DSL

A tiny and simple wrapper around `javax.sql.DataSource` to make working directly with it a bit more convenient in Kotlin.  

```text
implementation("io.github.nomisrev.jdbc:latest")
```

## Why?

In a lot of cases I've needed very little functionality, and simply wanted to run some simple queries against a database.
Using an ORM was unnecessary for my use-cases, and I've from time-to-time implemented small wrappers around JDBC like this.

So now I'm exposing it as a micro-lib, so I (and you) can depend on it from a common place and benefit from this DSL style.

## How

The DSL is exposes a `connection` DSL function on `javax.sql.DataSource`.

<!--- INCLUDE
import javax.sql.DataSource
-->
```kotlin
private val createUserTable: String =
  """CREATE TABLE IF NOT EXISTS users(
       id BIGSERIAL PRIMARY KEY,
       email VARCHAR(200) NOT NULL UNIQUE,
       username VARCHAR(100) NOT NULL UNIQUE
     )""".trimIndent()

fun DataSource.createTable(): Int =
  connection { update(createUserTable) }
```

<!--- INCLUDE
import javax.sql.DataSource

data class User(val id: Long, val email: String, val username: String)
-->
```kotlin
private val selectUser: String =
  """SELECT email, username
     FROM users
     WHERE id = ?;""".trimIndent()

fun DataSource.getUser(id: Long): User? =
  connection {
    queryOrNull(selectUser, { bind(id) }) {
      User(id = id, email= string(), username = string())
    }
  }
```
