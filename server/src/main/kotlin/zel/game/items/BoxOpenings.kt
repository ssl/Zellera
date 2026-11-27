package zel.game.items

import zel.db.allOrNothing
import zel.db.first
import zel.db.insertForId
import zel.db.write
import zel.game.players.ChatStyles
import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDate

/** Every step touches exactly one row, or the whole opening rolls back. */
object BoxOpenings {
    const val GIFT_CONTENT_OWNER = 0

    const val RECYCLER_PRIZE_SQL =
        "SELECT prize_definition_id, chat_bubble_id FROM recycler_boxes WHERE furni_id = ? LIMIT 1 FOR UPDATE"
    const val CLAIM_RECYCLER_PRIZE_SQL = "DELETE FROM recycler_boxes WHERE furni_id = ? LIMIT 1"
    const val DELETE_RECYCLER_BOX_SQL = "DELETE FROM furni WHERE id = ? AND player_id = ? LIMIT 1"
    const val INSERT_RECYCLER_PRIZE_SQL = "INSERT INTO furni (player_id, definition_id) VALUES (?, ?)"
    const val DELETE_OPENED_GIFT_SQL =
        "DELETE FROM furni WHERE id = ? AND player_id = ?" +
            " AND NOT EXISTS (SELECT 1 FROM wired_spawned_furni WHERE furni_id = furni.id) LIMIT 1"
    const val CLAIM_GIFT_CONTENT_SQL =
        "UPDATE furni SET player_id = ? WHERE id = ? AND player_id IS NULL AND room_id IS NULL" +
            " AND NOT EXISTS (SELECT 1 FROM marketplace_listings l WHERE l.furni_id = furni.id AND l.state <> 2)" +
            " AND NOT EXISTS (SELECT 1 FROM wired_chest_furni c WHERE c.furni_id = furni.id) LIMIT 1"
    const val OPENED_GIFT_CONTENT_SQL = "SELECT * FROM furni WHERE id = ? AND player_id = ? LIMIT 1"

    fun boxDay(date: LocalDate): String = date.toString()

    fun giftContents(ids: IntArray?): IntArray = ids?.filter { it > 0 }?.toSet()?.toIntArray() ?: IntArray(0)

    sealed interface Prize {
        data class Furni(
            val furniId: Int,
        ) : Prize

        data class ChatStyle(
            val styleId: Int,
        ) : Prize
    }

    /** Null when the box gives nothing; a chat style the opener owns already still uses the box up. */
    @Throws(SQLException::class)
    fun openRecyclerBox(
        connection: Connection,
        boxId: Int,
        openerId: Int,
        now: Int,
    ): Prize? =
        connection.allOrNothing {
            val (definitionId, styleId) =
                connection.first(RECYCLER_PRIZE_SQL, { int(boxId) }) {
                    it.getInt("prize_definition_id") to it.getInt("chat_bubble_id")
                } ?: return@allOrNothing null
            val boxGone =
                connection.oneRow(CLAIM_RECYCLER_PRIZE_SQL, boxId) &&
                    connection.oneRow(DELETE_RECYCLER_BOX_SQL, boxId, openerId)
            when {
                !boxGone -> {
                    null
                }

                styleId > 0 -> {
                    ChatStyles.grant(connection, openerId, styleId, now)
                    Prize.ChatStyle(styleId)
                }

                else -> {
                    connection
                        .insertForId(INSERT_RECYCLER_PRIZE_SQL) {
                            int(openerId)
                            int(definitionId)
                        }?.let(Prize::Furni)
                }
            }
        }

    @Throws(SQLException::class)
    fun openGift(
        connection: Connection,
        giftId: Int,
        openerId: Int,
        contentIds: IntArray,
    ): Boolean =
        connection.allOrNothing {
            val boxGone = connection.oneRow(DELETE_OPENED_GIFT_SQL, giftId, openerId)
            if (boxGone && contentIds.all { connection.oneRow(CLAIM_GIFT_CONTENT_SQL, openerId, it) }) true else null
        } ?: false

    private fun Connection.oneRow(
        sql: String,
        vararg ids: Int,
    ): Boolean = write(sql) { ids.forEach(::int) } == 1
}
