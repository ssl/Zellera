package zel.wired.store

import zel.db.Params
import java.sql.Connection

/** One update at a time: the driver refuses INSERT … SELECT as a batch. */
internal fun <T> Connection.writeRowByRow(
    sql: String,
    items: Iterable<T>,
    bind: Params.(T) -> Unit,
) {
    prepareStatement(sql).use { statement ->
        items.forEach { item ->
            Params(statement).bind(item)
            statement.executeUpdate()
        }
    }
}
