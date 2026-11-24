package zel.room.furni.behaviour.state

import zel.db.ParentRows
import zel.db.Sql
import javax.sql.DataSource

open class PlantStore(
    private val dataSource: DataSource? = null,
) {
    data class Growth(
        var frame: Int,
        var wateredAt: Long,
        var dead: Boolean,
    )

    data class Kind(
        val growFrames: Int,
        val deadFrame: Int,
    )

    open fun load(rowId: Int): Growth? =
        dataSource?.let {
            Sql(it).select(LOAD) { int(rowId) }.first { row ->
                Growth(row.getInt("count"), row.getLong("last_watered_at"), row.getInt("state") != 0)
            }
        }

    open fun save(
        rowId: Int,
        growth: Growth,
    ) = ParentRows.unlessGone {
        dataSource?.let {
            Sql(it).write(SAVE) {
                int(rowId)
                int(growth.frame)
                intOrNull(growth.wateredAt.toInt())
                int(if (growth.dead) 1 else 0)
            }
        }
    }

    open fun delete(rowId: Int) {
        dataSource?.let { Sql(it).write(DELETE) { int(rowId) } }
    }

    private companion object {
        const val LOAD = "SELECT count, last_watered_at, state FROM plants WHERE furni_id = ?"
        const val SAVE =
            "INSERT INTO plants (furni_id, count, last_watered_at, state) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE count = VALUES(count), " +
                "last_watered_at = VALUES(last_watered_at), state = VALUES(state)"
        const val DELETE = "DELETE FROM plants WHERE furni_id = ?"
    }
}
