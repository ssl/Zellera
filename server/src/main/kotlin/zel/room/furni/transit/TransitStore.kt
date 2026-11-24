package zel.room.furni.transit

import zel.db.Sql
import zel.db.first
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

class TransitStore(
    private val dataSource: DataSource,
) : TransitData {
    private val shown = ConcurrentHashMap<Int, Int>()

    override fun partnerOf(furniId: Int): Destination? =
        dataSource.connection.use { connection ->
            val row = BuildPassRowIds.rowOf(connection, furniId)
            one(connection, TARGET, row, row, row)
        }

    override fun aim(
        furniId: Int,
        targetId: Int,
    ) {
        dataSource.connection.use { connection ->
            val row = BuildPassRowIds.rowOf(connection, furniId)
            val target = BuildPassRowIds.rowOf(connection, targetId)
            if (pairOf(connection, row) == target) {
                connection.write(CLEAR_TARGET) { int(row) }
            } else {
                connection.write(SET_TARGET) {
                    int(row)
                    int(target)
                }
            }
        }
        shown.replace(furniId, targetId)
    }

    override fun loadTargets(roomId: Int) {
        Sql(dataSource).select(ROOM_TARGETS) { int(roomId) }.each { row -> shown[row.getInt(1)] = row.getInt(2) }
    }

    override fun shownTarget(furniId: Int): Int = shown.getOrPut(furniId) { partnerOf(furniId)?.furniId ?: 0 }

    override fun showTarget(
        furniId: Int,
        targetId: Int,
    ) {
        shown[furniId] = targetId
    }

    override fun forgetTargets(furniIds: Collection<Int>) {
        furniIds.forEach(shown::remove)
    }

    override fun otherHopper(
        definitionId: Int,
        roomId: Int,
    ): Destination? = dataSource.connection.use { one(it, OTHER_HOPPER, definitionId, roomId) }

    private fun pairOf(
        connection: Connection,
        row: Int,
    ): Int? = connection.first(PAIR, { repeat(PAIR_PARAMETERS) { int(row) } }) { it.getInt(1) }

    private fun one(
        connection: Connection,
        sql: String,
        vararg parameters: Int,
    ): Destination? =
        connection.first(sql, { parameters.forEach { int(it) } }) { row -> Destination(row.getInt(1), row.getInt(2)) }

    private companion object {
        const val PAIR_PARAMETERS = 2

        /** One keyed lookup for each side of the pair. */
        const val PAIR =
            "SELECT second_furni_id FROM teleport_pairs WHERE first_furni_id = ? " +
                "UNION ALL SELECT first_furni_id FROM teleport_pairs WHERE second_furni_id = ? LIMIT 1"
        const val TARGET =
            "SELECT ${BuildPassRowIds.CLIENT_FACING}, i.room_id FROM furni i ${BuildPassRowIds.JOIN} " +
                "WHERE i.id = COALESCE((SELECT target_furni_id FROM teleport_targets WHERE furni_id = ?), " +
                "($PAIR)) LIMIT 1"
        const val SET_TARGET =
            "INSERT INTO teleport_targets (furni_id, target_furni_id) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE target_furni_id = VALUES(target_furni_id)"
        const val CLEAR_TARGET = "DELETE FROM teleport_targets WHERE furni_id = ?"
        const val ROOM_TARGETS =
            "SELECT ${BuildPassRowIds.CLIENT_FACING}, COALESCE(tb.client_id, t.id, 0) FROM furni i " +
                "JOIN furni_definitions d ON d.id = i.definition_id ${BuildPassRowIds.JOIN} " +
                "LEFT JOIN teleport_targets tt ON tt.furni_id = i.id " +
                "LEFT JOIN teleport_pairs p1 ON p1.first_furni_id = i.id " +
                "LEFT JOIN teleport_pairs p2 ON p2.second_furni_id = i.id " +
                "LEFT JOIN furni t ON t.id = COALESCE(tt.target_furni_id, p1.second_furni_id, p2.first_furni_id) " +
                "LEFT JOIN build_pass_furni tb ON tb.furni_id = t.id " +
                "WHERE i.room_id = ? AND d.behaviour IN ('transit.door', 'transit.tile', 'transit.tile.club')"
        const val OTHER_HOPPER =
            "SELECT ${BuildPassRowIds.CLIENT_FACING}, i.room_id FROM furni i ${BuildPassRowIds.JOIN} " +
                "WHERE i.definition_id = ? AND i.room_id <> ? ORDER BY RAND() LIMIT 1"
    }
}
