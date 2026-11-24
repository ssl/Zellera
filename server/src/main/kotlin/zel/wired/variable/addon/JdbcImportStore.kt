package zel.wired.variable.addon

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import zel.db.Params
import zel.db.Sql
import zel.db.first
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import zel.wired.store.inOneTransaction
import zel.wired.variable.SharedToken
import zel.wired.variable.VariableTarget
import javax.sql.DataSource

/** Build Pass furni go by their client id. */
class JdbcImportStore(
    private val dataSource: DataSource,
) : ImportStore {
    private val sql = Sql(dataSource)

    override fun importsOf(
        roomId: Int,
        boxId: Int,
    ): List<ImportRow> =
        imports("$IMPORTS AND JSON_VALUE(w.variable_ids, '$[0]') IN (?, ?)") {
            string(SharedToken(VariableTarget.USER, roomId, boxId).text)
            string(SharedToken(VariableTarget.GLOBAL, roomId, boxId).text)
        }

    override fun importsIn(roomId: Int): List<ImportRow> = imports("$IMPORTS AND i.room_id = ?") { int(roomId) }

    private fun imports(
        query: String,
        bind: Params.() -> Unit,
    ): List<ImportRow> =
        sql.select(query, bind).rows { rows ->
            val shared = rows.getString("shared")?.let(SharedToken::parse)
            ImportRow(
                rows.getInt("room_id"),
                rows.getInt("import_id"),
                ImportStore.tokenOf(shared?.roomId ?: 0, shared?.boxId ?: 0),
            )
        }

    override fun sharedBy(token: String): ImportedAddons? {
        val (roomId, boxId) = token.split(':').map(String::toInt)
        val boxes =
            sql
                .select(TILE) {
                    int(boxId)
                    int(boxId)
                    int(roomId)
                }.rows { rows ->
                    TileRow(
                        rows.getInt("box_id"),
                        rows.getInt("definition_id"),
                        rows.getString("behaviour").orEmpty(),
                        rows.getString("ints")?.let { JsonParser.parseString(it).asJsonArray.map { int -> int.asInt } },
                        rows.getString("text").orEmpty(),
                    )
                }
        return read(boxes)
    }

    private class TileRow(
        val id: Int,
        val baseId: Int,
        val code: String,
        val ints: List<Int>?,
        val text: String,
    )

    private fun read(boxes: List<TileRow>): ImportedAddons? {
        val share = boxes.firstOrNull { it.code == AddonCodes.SHARE } ?: return null

        fun on(category: Int) = share.ints?.getOrNull(category) != 0

        fun copies(codes: (String) -> Boolean) =
            boxes
                .filter { codes(it.code) }
                .mapNotNull { box -> box.ints?.let { CopiedBox(box.baseId, box.id, it, box.text) } }
        return ImportedAddons(
            if (on(StackAddons.SHARED_CONNECTORS)) {
                boxes.filter { it.code == AddonCodes.TEXT_CONNECTOR }.map { it.text }
            } else {
                emptyList()
            },
            if (on(StackAddons.SHARED_LEVELS)) copies { it == AddonCodes.LEVEL_UP } else emptyList(),
            if (on(StackAddons.SHARED_FX)) copies { FxKind.fromCode(it) != null } else emptyList(),
        )
    }

    override fun save(
        row: ImportRow,
        addons: ImportedAddons,
    ) {
        dataSource.inOneTransaction { c ->
            val rowId = BuildPassRowIds.rowOf(c, row.importId)
            val keys =
                c.first(EXTRAS, { int(rowId) }) { rows ->
                    rows.getString(1)?.let { JsonParser.parseString(it) as? JsonObject }
                } ?: JsonObject()
            addons.writeTo(keys, row.token)
            c.write(SAVE_EXTRAS) {
                int(rowId)
                string(keys.toString())
            }
        }
    }

    private companion object {
        const val IMPORTS =
            "SELECT i.room_id, ${BuildPassRowIds.CLIENT_FACING} AS import_id, " +
                "JSON_VALUE(w.variable_ids, '$[0]') AS shared FROM furni i " +
                "${BuildPassRowIds.JOIN} " +
                "JOIN furni_definitions ib ON ib.id = i.definition_id AND ib.behaviour = '${AddonCodes.IMPORT}' " +
                "JOIN furni r ON r.room_id = i.room_id AND r.x = i.x AND r.y = i.y " +
                "JOIN furni_definitions rb ON rb.id = r.definition_id AND rb.behaviour = 'wf_var_reference' " +
                "LEFT JOIN wired_boxes w ON w.furni_id = r.id " +
                "WHERE i.room_id IS NOT NULL"
        const val TILE =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS box_id, i.definition_id, b.behaviour, w.ints, w.text " +
                "FROM furni i " +
                "${BuildPassRowIds.JOIN} JOIN furni_definitions b ON b.id = i.definition_id " +
                "JOIN furni s ON s.id = COALESCE((SELECT furni_id FROM build_pass_furni WHERE client_id = ?), ?) " +
                "AND s.room_id = i.room_id AND s.x = i.x AND s.y = i.y " +
                "LEFT JOIN wired_boxes w ON w.furni_id = i.id " +
                "WHERE i.room_id = ? ORDER BY i.z, i.id"

        const val EXTRAS = "SELECT extras FROM wired_boxes WHERE furni_id = ?"
        const val SAVE_EXTRAS =
            "INSERT INTO wired_boxes (furni_id, ints, text, furni_sources, player_sources, variable_ids, extras) " +
                "VALUES (?, '[]', '', '[]', '[]', '[]', ?) ON DUPLICATE KEY UPDATE extras = VALUES(extras)"
    }
}
