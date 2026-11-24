package zel.game.guilds

import org.slf4j.LoggerFactory
import zel.db.Params
import zel.db.Sql
import zel.db.first
import zel.db.orFallback
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

internal val guildStorageLog = LoggerFactory.getLogger("zel.game.guilds")

/** A failed read is logged and leaves [into] as it got. */
internal fun <T, C : MutableCollection<T>> DataSource.readInto(
    what: String,
    into: C,
    sql: String,
    bind: Params.() -> Unit = {},
    row: (ResultSet) -> T,
): C {
    guildStorageLog.orFallback("guild storage: $what", Unit) {
        Sql(this).select(sql, bind).each { into.add(row(it)) }
    }
    return into
}

internal fun Connection.countOf(
    sql: String,
    bind: Params.() -> Unit,
): Int = first(sql, bind) { it.getInt(1) } ?: 0
