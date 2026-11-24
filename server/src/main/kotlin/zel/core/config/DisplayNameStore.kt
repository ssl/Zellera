package zel.core.config

import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.Sql
import zel.db.SqlFailure

private val log = LoggerFactory.getLogger(DisplayNameStore::class.java)

class DisplayNameStore(
    private val database: Database,
) {
    fun readAll(): Map<String, String> =
        try {
            Sql(database.dataSource)
                .select("SELECT code, name FROM display_names")
                .rows { it.getString("code") to it.getString("name") }
                .toMap()
        } catch (e: SqlFailure) {
            log.error("display names not read; the build pass and the club go by their codes", e)
            emptyMap()
        }
}
