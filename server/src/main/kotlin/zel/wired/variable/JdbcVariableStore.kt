package zel.wired.variable

import com.google.gson.JsonParser
import zel.db.Params
import zel.db.ParentRows
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.rows
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import zel.wired.firing.HeldValue
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class JdbcVariableStore(
    private val dataSource: DataSource,
) : VariableStore {
    private val sql = Sql(dataSource)

    override fun roomValues(roomId: Int): StoredValues =
        dataSource.connection.use { c ->
            StoredValues(
                holderRows(c, USERS, roomId, null),
                holderRows(c, FURNI, roomId, null),
                roomRows(c, roomId, null),
            )
        }

    override fun boxValues(
        roomId: Int,
        boxId: Int,
    ): StoredValues =
        dataSource.connection.use { c ->
            StoredValues(
                holderRows(c, USERS, roomId, boxId),
                holderRows(c, FURNI, roomId, boxId),
                roomRows(c, roomId, boxId),
            )
        }

    private fun holderRows(
        c: Connection,
        query: String,
        roomId: Int,
        boxId: Int?,
    ): Map<Int, Map<Int, HeldValue>> {
        val out = HashMap<Int, HashMap<Int, HeldValue>>()
        c
            .rows(
                ofBox(query, boxId),
                { roomAndBox(roomId, boxId) },
            ) { Triple(it.getInt(BOX), it.getInt(HOLDER), held(it)) }
            .forEach { (box, holder, value) -> out.getOrPut(box) { HashMap() }[holder] = value }
        return out
    }

    private fun roomRows(
        c: Connection,
        roomId: Int,
        boxId: Int?,
    ): Map<Int, HeldValue> =
        c.rows(ofBox(ROOMS, boxId), { roomAndBox(roomId, boxId) }) { rows -> rows.getInt(BOX) to held(rows) }.toMap()

    private fun ofBox(
        query: String,
        boxId: Int?,
    ) = if (boxId == null) query else "$query AND variable_box_id = ?"

    private fun Params.roomAndBox(
        roomId: Int,
        boxId: Int?,
    ) {
        int(roomId)
        boxId?.let { int(it) }
    }

    private fun held(rows: ResultSet) =
        HeldValue(rows.getLong("value"), rows.getLong("created_at_ms"), rows.getLong("updated_at_ms"))

    override fun saveUser(
        roomId: Int,
        userId: Int,
        boxId: Int,
        value: Long?,
        held: HeldValue,
    ) = saveHolder(SAVE_USER, roomId, userId, boxId, value, held)

    override fun deleteUser(
        roomId: Int,
        userId: Int,
        boxId: Int,
    ) = deleteHolder(DELETE_USER, roomId, userId, boxId)

    override fun saveFurni(
        roomId: Int,
        furniId: Int,
        boxId: Int,
        value: Long?,
        held: HeldValue,
    ) = saveHolder(SAVE_FURNI, roomId, furniId, boxId, value, held) { c, id -> BuildPassRowIds.rowOf(c, id) }

    override fun deleteFurni(
        roomId: Int,
        furniId: Int,
        boxId: Int,
    ) = deleteHolder(DELETE_FURNI, roomId, furniId, boxId) { c, id -> BuildPassRowIds.rowOf(c, id) }

    private fun saveHolder(
        query: String,
        roomId: Int,
        holder: Int,
        boxId: Int,
        value: Long?,
        held: HeldValue,
        rowOf: (Connection, Int) -> Int = { _, id -> id },
    ) = ParentRows.unlessGone {
        dataSource.connection.use { c ->
            val row = rowOf(c, holder)
            c.write(query) {
                int(roomId)
                int(row)
                int(boxId)
                value(value)
                long(held.created)
                long(held.updated)
            }
        }
    }

    private fun deleteHolder(
        query: String,
        roomId: Int,
        holder: Int,
        boxId: Int,
        rowOf: (Connection, Int) -> Int = { _, id -> id },
    ) {
        dataSource.connection.use { c ->
            val row = rowOf(c, holder)
            c.write(query) {
                int(roomId)
                int(row)
                int(boxId)
            }
        }
    }

    override fun saveRoom(
        roomId: Int,
        boxId: Int,
        held: HeldValue,
    ) = ParentRows.unlessGone {
        sql.write(SAVE_ROOM) {
            int(roomId)
            int(boxId)
            long(held.value)
            long(held.created)
            long(held.updated)
        }
    }

    override fun deleteBox(
        roomId: Int,
        boxId: Int,
    ) {
        dataSource.connection.use { c ->
            c.allOrNothing {
                DELETE_BOX.forEach { table ->
                    c.write("DELETE FROM $table WHERE room_id = ? AND variable_box_id = ?") {
                        int(roomId)
                        int(boxId)
                    }
                }
            }
        }
    }

    override fun userNames(ids: Collection<Int>): Map<Int, String> {
        if (ids.isEmpty()) return emptyMap()
        val marks = ids.joinToString(",") { "?" }
        return sql
            .select("SELECT id, name FROM players WHERE id IN ($marks)") { ids.forEach { int(it) } }
            .rows { it.getInt(1) to it.getString(2) }
            .toMap()
    }

    override fun sharedOffers(
        ownerId: Int,
        exceptRoom: Int,
    ): List<SharedOffer> =
        sql
            .select(OFFERS) {
                intOrNull(ownerId)
                int(exceptRoom)
            }.rows(::offer)
            .filterNotNull()

    private fun offer(rows: ResultSet): SharedOffer? {
        val ints = JsonParser.parseString(rows.getString("ints")).asJsonArray.map { it.asInt }
        val name = rows.getString("text").orEmpty()
        val user = rows.getString("behaviour") == USER_BOX
        if (name.isEmpty() || ints.firstOrNull() != Availability.SHARED) return null
        return SharedOffer(
            rows.getInt("room_id"),
            rows.getString("name"),
            rows.getInt("id"),
            name,
            if (user) VariableTarget.USER else VariableTarget.GLOBAL,
        )
    }

    override fun sharedBoxExists(
        roomId: Int,
        boxId: Int,
    ): Boolean =
        sql
            .select("SELECT 1 FROM furni WHERE id = ? AND room_id = ?") {
                int(boxId)
                int(roomId)
            }.first { true } ?: false

    override fun sharedBoxInts(
        roomId: Int,
        boxId: Int,
    ): List<Int>? =
        sql
            .select(BOX_INTS) {
                int(boxId)
                int(roomId)
            }.first { rows ->
                rows.getString("ints")?.let { text -> JsonParser.parseString(text).asJsonArray.map { it.asInt } }
            }

    override fun userCard(id: Int): Pair<String, String>? =
        sql.select("SELECT name, figure FROM players WHERE id = ?") { int(id) }.first {
            it.getString(1) to
                it.getString(2)
        }

    private companion object {
        const val BOX = "variable_box_id"
        const val HOLDER = "holder_id"
        const val USER_BOX = "wf_var_user"
        const val USERS =
            "SELECT player_id AS holder_id, variable_box_id, value, created_at_ms, updated_at_ms " +
                "FROM wired_player_values WHERE room_id = ?"
        const val FURNI =
            "SELECT COALESCE(bc.client_id, v.furni_id) AS holder_id, variable_box_id, value, created_at_ms, " +
                "updated_at_ms FROM wired_furni_values v LEFT JOIN build_pass_furni bc ON bc.furni_id = v.furni_id " +
                "WHERE v.room_id = ?"
        const val ROOMS =
            "SELECT variable_box_id, value, created_at_ms, updated_at_ms FROM wired_room_values WHERE room_id = ?"
        const val SAVE_USER =
            "REPLACE INTO wired_player_values " +
                "(room_id, player_id, variable_box_id, value, created_at_ms, updated_at_ms) VALUES (?, ?, ?, ?, ?, ?)"
        const val SAVE_FURNI =
            "REPLACE INTO wired_furni_values " +
                "(room_id, furni_id, variable_box_id, value, created_at_ms, updated_at_ms) VALUES (?, ?, ?, ?, ?, ?)"
        const val SAVE_ROOM =
            "REPLACE INTO wired_room_values " +
                "(room_id, variable_box_id, value, created_at_ms, updated_at_ms) VALUES (?, ?, ?, ?, ?)"
        const val DELETE_USER =
            "DELETE FROM wired_player_values WHERE room_id = ? AND player_id = ? AND variable_box_id = ?"
        const val DELETE_FURNI =
            "DELETE FROM wired_furni_values WHERE room_id = ? AND furni_id = ? AND variable_box_id = ?"
        val DELETE_BOX = listOf("wired_player_values", "wired_furni_values", "wired_room_values")
        const val BOX_INTS =
            "SELECT a.ints FROM furni i JOIN wired_boxes a ON a.furni_id = i.id WHERE i.id = ? AND i.room_id = ?"
        const val OFFERS =
            "SELECT i.id, i.room_id, r.name, b.behaviour, a.ints, a.text FROM furni i " +
                "JOIN rooms r ON r.id = i.room_id JOIN furni_definitions b ON b.id = i.definition_id " +
                "JOIN wired_boxes a ON a.furni_id = i.id " +
                "WHERE r.owner_id <=> ? AND i.room_id <> ? AND b.behaviour IN ('wf_var_user', 'wf_var_room') " +
                "ORDER BY i.room_id, i.id"
    }
}
