package zel.game.effects

import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.Sql
import zel.db.SqlFailure

private val log = LoggerFactory.getLogger(EffectRoleStore::class.java)

class EffectRoleStore(
    private val database: Database,
) {
    fun readAll(): Map<String, Int> =
        try {
            Sql(database.dataSource)
                .select("SELECT role, effect_id FROM effect_roles")
                .rows { it.getString("role") to it.getInt("effect_id") }
                .toMap()
        } catch (e: SqlFailure) {
            log.error("effect roles not read; no role puts an effect on a unit", e)
            emptyMap()
        }
}
