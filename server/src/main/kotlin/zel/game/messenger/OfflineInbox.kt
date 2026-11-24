package zel.game.messenger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure

/**
 * The stored messages nobody was there to read. One login is handed at most [deliveryMax] of them, oldest first,
 * and one sender may leave a recipient at most [perSenderMax].
 */
class OfflineInbox(
    private val sql: Sql,
    private val deliveryMax: Int,
    private val clock: () -> Int,
) {
    val perSenderMax: Int = maxOf(1, deliveryMax / SENDER_SHARE)

    class Waiting(
        val row: Int,
        val message: Message,
    )

    /** An unreadable count lets nothing more in. */
    fun hasRoomFor(
        senderId: Int,
        recipientId: Int,
    ): Boolean =
        try {
            val waiting =
                sql
                    .select(COUNT_FROM) {
                        int(recipientId)
                        int(senderId)
                    }.first { it.getInt(1) } ?: 0
            waiting < perSenderMax
        } catch (failure: SqlFailure) {
            log.error("waiting messages from {} to {} not counted", senderId, recipientId, failure)
            false
        }

    fun waitingFor(recipientId: Int): List<Waiting>? =
        try {
            sql.select("$WAITING LIMIT $deliveryMax", recipientId).rows { row ->
                Waiting(
                    row.getInt("id"),
                    Message(
                        fromId = row.getInt("sender_id"),
                        toId = recipientId,
                        message = row.getString("message"),
                        timestamp = row.getInt("sent_at"),
                        iconId = row.getInt("icon_id"),
                        id = row.getString("message_code"),
                    ),
                )
            }
        } catch (failure: SqlFailure) {
            log.error("waiting messages of {} not read", recipientId, failure)
            null
        }

    /** Everything up to the last row handed over counts as delivered. */
    fun handedOver(
        recipientId: Int,
        handed: List<Waiting>,
    ) {
        val last = handed.maxOfOrNull { it.row } ?: return
        try {
            sql.write(MARK_DELIVERED) {
                int(clock())
                int(recipientId)
                int(last)
            }
        } catch (failure: SqlFailure) {
            log.error("waiting messages of {} not marked as delivered", recipientId, failure)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(OfflineInbox::class.java)

        /** One sender fills at most a tenth of what a login is handed. */
        const val SENDER_SHARE = 10
        const val COUNT_FROM =
            "SELECT COUNT(*) FROM direct_messages WHERE target_id = ? AND delivered_at IS NULL AND sender_id = ?"
        const val WAITING =
            "SELECT id, sender_id, message, icon_id, message_code, sent_at FROM direct_messages " +
                "WHERE target_id = ? AND delivered_at IS NULL ORDER BY id"
        const val MARK_DELIVERED =
            "UPDATE direct_messages SET delivered_at = ? WHERE target_id = ? AND delivered_at IS NULL AND id <= ?"
    }
}
