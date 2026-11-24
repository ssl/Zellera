package zel.game.bots

import zel.game.players.Gender
import java.sql.ResultSet
import java.sql.SQLException

/** Owned by the thread of the room that holds it. */
class Bot(
    val id: Int,
    val type: String,
    var name: String,
    var motto: String,
    var figure: String,
    var gender: Gender,
    val ownerId: Int,
    var ownerName: String?,
    var roams: Boolean,
    val speech: BotSpeech,
    val effect: Int,
    val bubble: Int,
) {
    private var lastOwnerChange = 0L

    val isPublic: Boolean get() = type == PUBLIC_KIND

    fun ownerMayChange(now: Long = System.currentTimeMillis()): Boolean {
        if (now - lastOwnerChange < OWNER_CHANGE_GAP_MS) return false
        lastOwnerChange = now
        return true
    }

    companion object {
        const val OWNER_CHANGE_GAP_MS = 500L

        const val PUBLIC_KIND = "public"

        @Throws(SQLException::class)
        fun read(row: ResultSet): Bot =
            Bot(
                id = row.getInt("id"),
                type = row.getString("kind").orEmpty(),
                name = row.getString("name").orEmpty(),
                motto = row.getString("motto").orEmpty(),
                figure = row.getString("figure").orEmpty(),
                gender = Gender.ofCode(row.getString("gender").orEmpty()) ?: Gender.M,
                ownerId = row.getInt("player_id"),
                ownerName = row.getString("owner_name"),
                roams = row.getBoolean("roams"),
                speech =
                    BotSpeech(
                        BotChatLines.unpack(row.getString(BotChatLines.COLUMN_NAME)),
                        row.getBoolean("chat_auto"),
                        row.getBoolean("chat_random"),
                        row.getShort("chat_delay_seconds"),
                    ),
                effect = row.getInt("effect_id"),
                bubble = row.getInt("bubble_id"),
            )
    }
}
