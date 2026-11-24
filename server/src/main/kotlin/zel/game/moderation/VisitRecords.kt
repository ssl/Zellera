package zel.game.moderation

import zel.core.rules.rules
import zel.db.Sql
import javax.sql.DataSource

class VisitRecords(
    dataSource: DataSource,
    private val chat: ChatRecords,
    private val now: () -> Int,
    private val visitLimit: () -> Int = { rules.limits.moderationRoomVisits },
) {
    private val queries = Sql(dataSource)

    data class Visitor(
        val name: String,
        val enteredAt: Int,
    )

    fun visitsOf(playerId: Int): List<RoomVisit> = visits(playerId, visitLimit()).map { it.first }

    fun visitsWithChat(playerId: Int): List<RoomVisit> =
        visits(playerId, VISITS_WITH_CHAT).map { (visit, leftAt) ->
            visit.copy(chat = chat.roomLinesBetween(visit.roomId, visit.timestamp, leftAt ?: now()))
        }

    /** Who entered the room since [since], once each at the latest entry, newest first, without [excludeName]. */
    fun recentVisitors(
        roomId: Int,
        since: Int,
        excludeName: String,
        limit: Int,
    ): List<Visitor> =
        queries
            .select(RECENT_VISITORS) {
                int(roomId)
                int(since)
                string(excludeName)
                int(limit)
            }.rows { Visitor(it.getString("name"), it.getLong("last_entry").toInt()) }

    private fun visits(
        playerId: Int,
        limit: Int,
    ): List<Pair<RoomVisit, Int?>> =
        queries
            .select(
                "SELECT v.room_id, r.name, v.entered_at, v.left_at FROM room_visits v " +
                    "LEFT JOIN rooms r ON r.id = v.room_id " +
                    "WHERE v.player_id = ? ORDER BY v.entered_at DESC, v.id DESC LIMIT ?",
            ) {
                int(playerId)
                int(limit)
            }.rows { row ->
                val leftAt = row.getLong("left_at").takeUnless { row.wasNull() }?.toInt()
                val enteredAt = row.getLong("entered_at").toInt()
                val visit = RoomVisit(row.getInt("room_id"), row.getString("name").orEmpty(), enteredAt)
                visit to leftAt
            }

    internal companion object {
        private const val VISITS_WITH_CHAT = 10
        const val RECENT_VISITORS =
            "SELECT p.name, MAX(v.entered_at) AS last_entry FROM room_visits v JOIN players p ON p.id = v.player_id " +
                "WHERE v.room_id = ? AND v.entered_at >= ? AND p.name <> ? " +
                "GROUP BY v.player_id, p.name ORDER BY last_entry DESC, v.player_id DESC LIMIT ?"
    }
}
