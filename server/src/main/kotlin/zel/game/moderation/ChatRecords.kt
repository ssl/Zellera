package zel.game.moderation

import zel.core.rules.rules
import zel.db.Sql
import java.sql.ResultSet
import javax.sql.DataSource

/** Logged room and private chat for moderators: the newest lines up to the chat log limit, oldest first. */
class ChatRecords(
    dataSource: DataSource,
    private val lineLimit: () -> Int = { rules.limits.moderationChatlogLines },
) {
    private val queries = Sql(dataSource)

    fun roomLines(roomId: Int): List<ChatlogLine> = room("c.room_id = ?", roomId)

    fun linesBy(playerId: Int): List<ChatlogLine> = room("c.sender_id = ?", playerId)

    /** [from] and [until] are both inclusive. */
    fun roomLinesBetween(
        roomId: Int,
        from: Int,
        until: Int,
    ): List<ChatlogLine> = room("c.room_id = ? AND c.sent_at BETWEEN ? AND ?", roomId, from, until)

    fun conversation(
        one: Int,
        other: Int,
    ): List<ChatlogLine> =
        newestFirst(
            "SELECT c.sent_at, c.sender_id, p.name, c.message FROM direct_messages c " +
                "LEFT JOIN players p ON p.id = c.sender_id WHERE c.icon_id IS NULL AND " +
                "((c.sender_id = ? AND c.target_id = ?) OR (c.sender_id = ? AND c.target_id = ?)) " +
                "ORDER BY c.sent_at DESC, c.id DESC LIMIT ?",
            one,
            other,
            other,
            one,
        ).asReversed()

    private fun room(
        where: String,
        vararg values: Int,
    ): List<ChatlogLine> =
        newestFirst(
            "SELECT c.sent_at, c.sender_id, p.name, c.message FROM room_chat_messages c " +
                "LEFT JOIN players p ON p.id = c.sender_id WHERE $where ORDER BY c.sent_at DESC, c.id DESC LIMIT ?",
            *values,
        ).asReversed()

    private fun newestFirst(
        sql: String,
        vararg values: Int,
    ): List<ChatlogLine> =
        queries
            .select(sql) {
                values.forEach(::int)
                int(lineLimit())
            }.rows(::lineOf)

    private fun lineOf(row: ResultSet) =
        ChatlogLine(
            row.getLong("sent_at").toInt(),
            row.getInt("sender_id"),
            row.getString("name").orEmpty(),
            row.getString("message").orEmpty(),
        )
}
