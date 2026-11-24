package zel.wired.store

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import zel.db.Sql
import zel.db.rows
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import zel.wired.box.BoxSettings
import zel.wired.box.FurniSnapshot
import zel.wired.form.WiredMasks
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.math.abs

class JdbcWiredStore(
    private val dataSource: DataSource,
) : WiredStore {
    private val sql = Sql(dataSource)

    override fun roomBoxes(roomId: Int): Map<Int, StoredBox> =
        dataSource.connection.use { c ->
            val picks =
                c
                    .rows(ROOM_PICKS, { int(roomId) }) { it.getInt("box") to pick(it) }
                    .groupBy({ it.first }, { it.second })
            c
                .rows(ROOM_BOXES, { int(roomId) }) { rows ->
                    val id = rows.getInt("id")
                    id to box(rows, picks[id].orEmpty())
                }.toMap()
        }

    private class PickRow(
        val list: Int,
        val id: Int,
        val state: String?,
        val snapshot: FurniSnapshot?,
    )

    private data class PickLine(
        val list: Int,
        val position: Int,
        val pick: Int,
        val settings: BoxSettings?,
    )

    private fun pick(rows: ResultSet): PickRow {
        val id = rows.getInt("pick")
        val state = rows.getString("saved_state")
        val x = rows.getInt("saved_x").takeUnless { rows.wasNull() }
        return PickRow(
            rows.getInt("list"),
            if (rows.getBoolean("wall")) -id else id,
            state,
            x?.let {
                FurniSnapshot(
                    id,
                    state.orEmpty(),
                    rows.getInt("saved_direction"),
                    it,
                    rows.getInt("saved_y"),
                    rows.getDouble("saved_z"),
                    rows.getString("saved_wall_position"),
                )
            },
        )
    }

    private fun box(
        rows: ResultSet,
        picks: List<PickRow>,
    ): StoredBox {
        val first = picks.filter { it.list == FIRST_LIST }
        return StoredBox(
            BoxSettings(
                ints = ints(rows.getString("ints")),
                text = rows.getString("text").orEmpty(),
                picks = first.map { it.id },
                picks2 = picks.filter { it.list == SECOND_LIST }.map { it.id },
                furniSources = ints(rows.getString("furni_sources")),
                userSources = ints(rows.getString("player_sources")),
                quantifier = rows.getInt("quantifier"),
                delay = rows.getInt("delay_half_seconds"),
                variableIds = texts(rows.getString("variable_ids")),
                filter = rows.getBoolean("filter_existing"),
                invert = rows.getBoolean("invert"),
                furniStates =
                    first
                        .filter { it.snapshot == null && it.state != null }
                        .associate { it.id to it.state.orEmpty() },
                snapshots = first.mapNotNull { it.snapshot },
            ),
            rows.getBoolean("is_fresh"),
            rows.getString("extras")?.let { JsonParser.parseString(it) as? JsonObject },
        )
    }

    override fun saveBox(
        furniId: Int,
        box: StoredBox,
    ) = dataSource.inOneTransaction { c ->
        val rowId = BuildPassRowIds.rowOf(c, furniId)
        writeForm(c, rowId, box.settings, box.isFresh)
        writeExtras(c, rowId, box.extras)
    }

    private fun writeForm(
        c: Connection,
        rowId: Int,
        s: BoxSettings,
        isFresh: Boolean,
    ) {
        c.write(SAVE_FORM) {
            boolean(isFresh)
            string(json(s.ints))
            string(s.text)
            string(json(s.furniSources))
            string(json(s.userSources))
            string(JsonArray().also { a -> s.variableIds.forEach(a::add) }.toString())
            int(tiny(s.quantifier))
            int(tiny(s.delay))
            boolean(s.filter)
            boolean(s.invert)
            int(rowId)
        }
        c.write(DELETE_PICKS) { int(rowId) }
        val lines =
            s.picks.mapIndexed { position, pick -> PickLine(FIRST_LIST, position, pick, s) } +
                s.picks2.mapIndexed { position, pick -> PickLine(SECOND_LIST, position, pick, null) }
        c.writeRowByRow(SAVE_PICK, lines) { line ->
            val id = abs(line.pick)
            val snapshot = line.settings?.snapshots?.firstOrNull { it.furniId == id }
            int(line.list)
            int(line.position)
            string(snapshot?.state ?: line.settings?.furniStates?.get(line.pick))
            value(snapshot?.x)
            value(snapshot?.y)
            value(snapshot?.z)
            value(snapshot?.direction)
            string(snapshot?.wall)
            int(rowId)
            int(id)
            int(id)
        }
    }

    private fun writeExtras(
        c: Connection,
        rowId: Int,
        extras: JsonObject?,
    ) {
        c.write(SAVE_EXTRAS) {
            string(extras?.toString())
            int(rowId)
        }
    }

    override fun masks(roomId: Int): WiredMasks? =
        sql.select(MASKS) { int(roomId) }.first { rs ->
            WiredMasks(rs.getInt("editors"), rs.getInt("readers"), rs.getString("clock_zone").orEmpty())
        }

    override fun saveMasks(
        roomId: Int,
        masks: WiredMasks,
    ) {
        sql.write(SAVE_MASKS) {
            int(roomId)
            int(masks.read)
            int(masks.modify)
            stringOrNull(masks.timezone)
        }
    }

    private fun json(values: List<Int>): String = JsonArray().also { a -> values.forEach(a::add) }.toString()

    private fun tiny(value: Int): Int = value.coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt())

    private fun ints(text: String?): List<Int> = array(text).map { it.asInt }

    private fun texts(text: String?): List<String> = array(text).map { it.asString }

    private fun array(text: String?): JsonArray = text?.let { JsonParser.parseString(it) as? JsonArray } ?: JsonArray()

    private companion object {
        const val FIRST_LIST = 1
        const val SECOND_LIST = 2
        const val ROOM_BOXES =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS id, b.is_fresh, b.ints, b.text, b.furni_sources, " +
                "b.player_sources, b.variable_ids, b.quantifier, b.delay_half_seconds, b.filter_existing, b.invert, " +
                "b.extras FROM wired_boxes b JOIN furni i ON i.id = b.furni_id ${BuildPassRowIds.JOIN} " +
                "WHERE i.room_id = ?"
        const val ROOM_PICKS =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS box, p.list, COALESCE(pbc.client_id, f.id) AS pick, " +
                "t.kind = 'i' AS wall, p.saved_state, p.saved_x, p.saved_y, p.saved_z, p.saved_direction, " +
                "p.saved_wall_position FROM wired_box_picks p JOIN furni i ON i.id = p.box_id " +
                "${BuildPassRowIds.JOIN} JOIN furni f ON f.id = p.furni_id " +
                "LEFT JOIN build_pass_furni pbc ON pbc.furni_id = f.id " +
                "JOIN furni_definitions t ON t.id = f.definition_id " +
                "WHERE i.room_id = ? ORDER BY p.box_id, p.list, p.position"
        const val SAVE_FORM =
            "INSERT INTO wired_boxes (furni_id, is_fresh, ints, text, furni_sources, player_sources, variable_ids, " +
                "quantifier, delay_half_seconds, filter_existing, invert) " +
                "SELECT i.id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? FROM furni i WHERE i.id = ? " +
                "ON DUPLICATE KEY UPDATE is_fresh = VALUES(is_fresh), ints = VALUES(ints), text = VALUES(text), " +
                "furni_sources = VALUES(furni_sources), player_sources = VALUES(player_sources), " +
                "variable_ids = VALUES(variable_ids), quantifier = VALUES(quantifier), " +
                "delay_half_seconds = VALUES(delay_half_seconds), filter_existing = VALUES(filter_existing), " +
                "invert = VALUES(invert)"
        const val DELETE_PICKS = "DELETE FROM wired_box_picks WHERE box_id = ?"
        const val SAVE_PICK =
            "INSERT INTO wired_box_picks (box_id, list, position, saved_state, saved_x, saved_y, saved_z, " +
                "saved_direction, saved_wall_position, furni_id) SELECT b.furni_id, ?, ?, ?, ?, ?, ?, ?, ?, f.id " +
                "FROM wired_boxes b JOIN furni f WHERE b.furni_id = ? " +
                "AND f.id = COALESCE((SELECT furni_id FROM build_pass_furni WHERE client_id = ?), ?)"
        const val SAVE_EXTRAS = "UPDATE wired_boxes SET extras = ? WHERE furni_id = ?"
        const val MASKS = "SELECT readers, editors, clock_zone FROM wired_room_settings WHERE room_id = ?"
        const val SAVE_MASKS =
            "REPLACE INTO wired_room_settings (room_id, readers, editors, clock_zone) VALUES (?, ?, ?, ?)"
    }
}
