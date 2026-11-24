package zel.game.bots

import org.slf4j.LoggerFactory
import zel.db.first
import zel.db.insertForId
import zel.db.orFallback
import java.sql.Connection
import java.sql.SQLException

class Bots(
    private val environment: BotEnvironment,
) {
    val butlerWords: Map<String, Int> by lazy {
        LOGGER.orFallback("reading the butler words", emptyMap()) { environment.connection().use(::readButlerWords) }
    }

    fun create(
        look: BotLook,
        kind: String,
        ownerId: Int,
    ): Bot? =
        LOGGER.orFallback("making a $kind bot for player $ownerId", null) {
            environment.connection().use { create(it, look, kind, ownerId) }
        }

    /** The name is cut to its limit; a gender other than `F` saves as `M`. */
    @Throws(SQLException::class)
    fun create(
        connection: Connection,
        look: BotLook,
        kind: String,
        ownerId: Int,
    ): Bot? {
        if (ownerId <= 0) return null
        val id =
            connection.insertForId(INSERT_SQL) {
                int(ownerId)
                string(look.name.take(BotLimits.maxNameLength))
                string(look.motto)
                string(look.figure)
                string(if (look.gender.equals("F", ignoreCase = true)) "F" else "M")
                string(kind)
            } ?: return null
        return connection.first(ONE_SQL, { int(id) }, Bot::read)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(Bots::class.java)
        private const val INSERT_SQL =
            "INSERT INTO bots (player_id, name, motto, figure, gender, kind) VALUES (?, ?, ?, ?, ?, ?)"

        /** A bot with its owner's name and its chat lines packed into one column. */
        const val SELECT =
            "SELECT bots.*, owners.name AS owner_name, ${BotChatLines.COLUMN} " +
                "FROM bots LEFT JOIN players owners ON owners.id = bots.player_id"

        const val ONE_SQL = "$SELECT WHERE bots.id = ?"

        const val OWNED_SQL = "$SELECT WHERE bots.player_id = ? AND bots.room_id IS NULL ORDER BY bots.id"
    }
}
