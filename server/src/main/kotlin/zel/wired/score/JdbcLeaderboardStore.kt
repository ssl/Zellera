package zel.wired.score

import zel.db.Sql
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import javax.sql.DataSource

class JdbcLeaderboardStore(
    private val dataSource: DataSource,
) : LeaderboardStore {
    private val sql = Sql(dataSource)

    override fun holders(
        roomId: Int,
        boxId: Int,
    ): List<LeaderHolder> =
        sql
            .select(HOLDERS) {
                int(roomId)
                int(boxId)
            }.rows { rs ->
                val value = rs.getLong("value").takeUnless { rs.wasNull() }
                LeaderHolder(
                    rs.getInt("player_id"),
                    rs.getString("name").orEmpty(),
                    rs.getString("figure").orEmpty(),
                    value,
                    rs.getLong("created_at_ms"),
                    rs.getLong("updated_at_ms"),
                )
            }

    override fun marks(roomId: Int): Map<Int, Pair<Int, Int>> =
        sql
            .select(MARKS) { int(roomId) }
            .rows { rs ->
                val revision = rs.getInt("revision").takeUnless { rs.wasNull() } ?: 1
                rs.getInt("box") to (rs.getInt("published") to revision)
            }.toMap()

    override fun saveMark(
        boxId: Int,
        publishedId: Int,
        revision: Int,
    ) {
        dataSource.connection.use { c ->
            c.write(MARK) {
                int(publishedId)
                int(revision)
                int(BuildPassRowIds.rowOf(c, boxId))
            }
        }
    }

    private companion object {
        const val MARK =
            "UPDATE wired_boxes SET extras = JSON_SET(COALESCE(extras, '{}'), '$.publishedId', ?, '$.revision', ?) " +
                "WHERE furni_id = ?"
        const val HOLDERS =
            "SELECT v.player_id, u.name, u.figure, v.value, v.created_at_ms, v.updated_at_ms " +
                "FROM wired_player_values v JOIN players u ON u.id = v.player_id " +
                "WHERE v.room_id = ? AND v.variable_box_id = ?"
        const val MARKS =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS box, JSON_VALUE(w.extras, '$.publishedId') AS published, " +
                "JSON_VALUE(w.extras, '$.revision') AS revision FROM wired_boxes w JOIN furni i ON i.id = w.furni_id " +
                "${BuildPassRowIds.JOIN} WHERE i.room_id = ? AND JSON_VALUE(w.extras, '$.publishedId') IS NOT NULL"
    }
}
