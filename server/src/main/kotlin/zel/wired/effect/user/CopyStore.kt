package zel.wired.effect.user

import zel.db.DeadlockRetry
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.first
import zel.db.rows
import zel.db.write
import zel.room.persist.FurniHandBack
import zel.room.persist.FurniTransfers
import java.sql.Connection
import javax.sql.DataSource

class CopyStore(
    private val dataSource: DataSource,
    private val next: FurniTransfers,
) : FurniTransfers by next {
    private val sql = Sql(dataSource)

    fun create(
        place: CopyPlace,
        make: (Connection) -> Int?,
    ): Int? =
        DeadlockRetry.run {
            dataSource.connection.use { c ->
                c.allOrNothing {
                    make(c)?.also { id ->
                        c.write(PLACE) {
                            int(place.roomId)
                            int(place.x)
                            int(place.y)
                            double(place.z)
                            int(place.direction)
                            int(id)
                        }
                        c.write(MARK) {
                            int(id)
                            int(place.roomId)
                        }
                    }
                }
            }
        }

    fun ofRoom(roomId: Int): Set<Int> = sql.select(MARKED) { int(roomId) }.rows { it.getInt(1) }.toSet()

    fun destroy(rowIds: List<Int>) {
        if (rowIds.isEmpty()) return
        val marks = marks(rowIds)
        DeadlockRetry.run {
            dataSource.connection.use { c ->
                c.allOrNothing {
                    DELETES.forEach { table -> c.write("$table ($marks)") { rowIds.forEach { int(it) } } }
                    Unit
                }
            }
        }
    }

    override fun takeToInventory(
        roomId: Int,
        furniId: Int,
        state: String,
    ): Boolean {
        val marked = dataSource.connection.use { c -> c.first(IS_MARKED, { int(furniId) }) { true } } == true
        if (marked) {
            destroy(listOf(furniId))
            return false
        }
        return next.takeToInventory(roomId, furniId, state)
    }

    override fun takeAllToInventory(
        roomId: Int,
        furni: List<FurniHandBack>,
    ): List<Int> {
        if (furni.isEmpty()) return emptyList()
        val marked = marked(furni.map { it.id })
        destroy(marked)
        return next.takeAllToInventory(roomId, furni.filterNot { it.id in marked })
    }

    private fun marked(rowIds: List<Int>): List<Int> =
        dataSource.connection.use { c ->
            c.rows("$MARKED_AMONG (${marks(rowIds)})", { rowIds.forEach { int(it) } }) { it.getInt(1) }
        }

    private fun marks(ids: List<Int>): String = ids.joinToString(",") { "?" }

    private companion object {
        const val PLACE =
            "UPDATE furni SET room_id = ?, x = ?, y = ?, z = ?, direction = ?, wall_position = '' WHERE id = ?"
        const val MARK = "INSERT IGNORE INTO wired_spawned_furni (furni_id, room_id) VALUES (?, ?)"
        const val MARKED =
            "SELECT COALESCE(bc.client_id, s.furni_id) FROM wired_spawned_furni s " +
                "LEFT JOIN build_pass_furni bc ON bc.furni_id = s.furni_id WHERE s.room_id = ?"
        const val IS_MARKED = "SELECT 1 FROM wired_spawned_furni WHERE furni_id = ?"
        const val MARKED_AMONG = "SELECT furni_id FROM wired_spawned_furni WHERE furni_id IN"
        val DELETES =
            listOf(
                "DELETE FROM wired_spawned_furni WHERE furni_id IN",
                "DELETE FROM build_pass_furni WHERE furni_id IN",
                "DELETE FROM furni WHERE id IN",
            )
    }
}
