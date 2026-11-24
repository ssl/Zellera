package zel.db

import java.sql.SQLException

class SqlFailure(
    sql: String,
    cause: SQLException,
) : SQLException("Statement failed: $sql", cause.sqlState, cause.errorCode, cause)
