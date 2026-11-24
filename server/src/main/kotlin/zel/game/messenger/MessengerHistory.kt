package zel.game.messenger

import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure
import zel.db.noneAsNull
import java.sql.ResultSet

/** The stored chats; a page is the [PAGE_SIZE] newest messages older than a given id, oldest first. */
class MessengerHistory(
    private val sql: Sql,
) {
    data class Entry(
        val senderId: Int,
        val senderName: String,
        val senderFigure: String,
        val text: String,
        val iconId: Int,
        val timestamp: Int,
        val messageId: String,
    )

    /** A message nobody was there to read is stored without a delivery time; false when it is not stored. */
    fun store(
        message: Message,
        delivered: Boolean,
    ): Boolean =
        try {
            sql.write(
                STORE,
                message.fromId,
                message.toId,
                message.id,
                if (message.isIcon) "" else message.message,
                noneAsNull(message.iconId),
                message.timestamp,
                if (delivered) message.timestamp else null,
            )
            true
        } catch (e: SqlFailure) {
            LOGGER.error("messenger message {} not stored", message.id, e)
            false
        }

    fun page(
        userId: Int,
        friendId: Int,
        beforeMessageId: String?,
    ): List<Entry> {
        if (userId == friendId || userId <= 0 || friendId <= 0) return emptyList()
        return try {
            sql
                .select(
                    PAGE,
                    userId,
                    friendId,
                    friendId,
                    userId,
                    beforeMessageId.orEmpty(),
                    userId,
                    friendId,
                    friendId,
                    userId,
                ).rows(::entry)
                .reversed()
        } catch (e: SqlFailure) {
            LOGGER.error("Could not read the messenger history", e)
            emptyList()
        }
    }

    private fun entry(set: ResultSet) =
        Entry(
            set.getInt("sender_id"),
            set.getString("name").orEmpty(),
            set.getString("figure").orEmpty(),
            set.getString("message"),
            set.getInt("icon_id"),
            set.getInt("sent_at"),
            set.getString("message_code"),
        )

    companion object {
        const val PAGE_SIZE = 30

        private const val STORE =
            "INSERT INTO direct_messages (sender_id, target_id, message_code, message, icon_id, sent_at, " +
                "delivered_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
        private const val CHAT =
            "((h.sender_id = ? AND h.target_id = ?) OR (h.sender_id = ? AND h.target_id = ?))"
        private const val PAGE =
            "SELECT h.sender_id, h.message_code, h.message, h.icon_id, h.sent_at, u.name, u.figure " +
                "FROM direct_messages h LEFT JOIN players u ON u.id = h.sender_id WHERE " + CHAT +
                " AND h.id < COALESCE((SELECT h.id FROM direct_messages h WHERE h.message_code = ? AND " + CHAT +
                " LIMIT 1), 2147483647) ORDER BY h.id DESC LIMIT " + PAGE_SIZE
        private val LOGGER = LoggerFactory.getLogger(MessengerHistory::class.java)
    }
}
