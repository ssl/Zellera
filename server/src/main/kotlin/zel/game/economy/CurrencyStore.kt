package zel.game.economy

import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.Sql
import zel.db.SqlFailure

private val log = LoggerFactory.getLogger(CurrencyStore::class.java)

class CurrencyStore(
    private val database: Database,
) {
    fun readAll(): List<Currency> =
        try {
            Sql(database.dataSource).select("SELECT id, code, name, points, role FROM currencies ORDER BY id").rows {
                Currency(
                    it.getInt("id"),
                    it.getString("code"),
                    it.getString("name"),
                    it.getBoolean("points"),
                    CurrencyRole.stored(it.getString("role")),
                )
            }
        } catch (e: SqlFailure) {
            log.error("currencies not read; no currency has a name", e)
            emptyList()
        }
}
