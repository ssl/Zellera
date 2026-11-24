package zel.wired.addon.label

import zel.db.Sql
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import javax.sql.DataSource

class JdbcLabelStore(
    private val dataSource: DataSource,
) : LabelStore {
    private val sql = Sql(dataSource)

    override fun load(furniIds: Collection<Int>): Map<Int, Label> {
        if (furniIds.isEmpty()) return emptyMap()
        val marks = furniIds.joinToString(",") { "?" }
        return sql
            .select("$LOAD ($marks)") { furniIds.forEach { int(it) } }
            .rows { rs ->
                rs.getInt("furni_id") to
                    Label(
                        rs.getString("text").orEmpty(),
                        rs.getInt("color"),
                        rs.getInt("visibility"),
                        rs.getBoolean("always_open"),
                    )
            }.toMap()
    }

    override fun save(
        furniId: Int,
        label: Label,
    ) {
        dataSource.connection.use { c ->
            c.write(SAVE) {
                int(BuildPassRowIds.rowOf(c, furniId))
                string(label.text)
                int(label.color)
                int(label.visibility)
                boolean(label.alwaysOpen)
            }
        }
    }

    override fun delete(furniId: Int) {
        dataSource.connection.use { c -> c.write(DELETE) { int(BuildPassRowIds.rowOf(c, furniId)) } }
    }

    private companion object {
        const val LOAD =
            "SELECT COALESCE(bc.client_id, l.furni_id) AS furni_id, l.text, l.color, l.visibility, l.always_open " +
                "FROM furni_labels l LEFT JOIN build_pass_furni bc ON bc.furni_id = l.furni_id " +
                "WHERE COALESCE(bc.client_id, l.furni_id) IN"
        const val SAVE =
            "INSERT INTO furni_labels (furni_id, text, color, visibility, always_open) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE text = VALUES(text), color = VALUES(color), " +
                "visibility = VALUES(visibility), always_open = VALUES(always_open)"
        const val DELETE = "DELETE FROM furni_labels WHERE furni_id = ?"
    }
}
