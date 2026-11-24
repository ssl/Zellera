package zel.db

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

// Columns that store NULL where the code uses a stand-in value such as 0 or -1.

fun noneAsNull(
    value: Int,
    none: Int = 0,
): Int? = value.takeUnless { it == none }

fun PreparedStatement.setNoneAsNull(
    index: Int,
    value: Int,
    none: Int = 0,
) {
    if (value == none) setNull(index, Types.INTEGER) else setInt(index, value)
}

fun PreparedStatement.setNotPositiveAsNull(
    index: Int,
    value: Int,
) {
    if (value <= 0) setNull(index, Types.INTEGER) else setInt(index, value)
}

fun PreparedStatement.setEmptyAsNull(
    index: Int,
    value: String?,
) {
    if (value.isNullOrEmpty()) setNull(index, Types.VARCHAR) else setString(index, value)
}

fun ResultSet.intOrNone(
    column: String,
    none: Int = 0,
): Int {
    val value = getInt(column)
    return if (wasNull()) none else value
}
