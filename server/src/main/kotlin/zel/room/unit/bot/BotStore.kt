package zel.room.unit.bot

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import zel.db.write
import zel.game.bots.Bot
import zel.game.bots.BotChatLines
import zel.game.bots.Bots
import zel.room.map.Point
import java.sql.ResultSet
import javax.sql.DataSource

class BotStore(
    private val dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    data class Stored(
        val bot: Bot,
        val tile: Point,
        val z: Double,
        val direction: Int,
        val dance: Int,
    )

    /** What a save writes, copied under the room lock. */
    data class Row(
        val id: Int,
        val name: String,
        val motto: String,
        val figure: String,
        val gender: String,
        val roomId: Int,
        val x: Int,
        val y: Int,
        val z: Double,
        val direction: Int,
        val dance: Int,
        val walks: Boolean,
        val chatLines: List<String>,
        val chatAuto: Boolean,
        val chatRandom: Boolean,
        val chatDelay: Int,
    )

    fun loadRoom(
        roomId: Int,
        build: (ResultSet) -> Bot?,
    ): List<Stored> =
        log.orFallback("loading the bots of room $roomId", emptyList()) {
            sql
                .select(ROOM_SQL, roomId)
                .rows { set ->
                    build(set)?.let { bot ->
                        Stored(
                            bot,
                            Point(set.getInt("x"), set.getInt("y")),
                            set.getDouble("z"),
                            set.getInt("direction"),
                            set.getInt("dance"),
                        )
                    }
                }.filterNotNull()
        }

    fun save(row: Row) = saveAll(listOf(row))

    fun saveAll(rows: List<Row>) {
        log.orFallback("saving bots ${rows.map(Row::id)}", Unit) {
            dataSource.connection.use { connection ->
                rows.forEach { row ->
                    BotChatLines.rewrite(connection, row.id, row.chatLines) {
                        connection.write(UPDATE_SQL) {
                            intOrNull(row.roomId)
                            int(row.x)
                            int(row.y)
                            double(row.z)
                            int(row.direction)
                            int(row.dance)
                            string(row.name)
                            string(row.motto)
                            string(row.figure)
                            string(row.gender)
                            boolean(row.walks)
                            boolean(row.chatAuto)
                            boolean(row.chatRandom)
                            int(row.chatDelay)
                            int(row.id)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(BotStore::class.java)

        const val ROOM_SQL = Bots.SELECT + " WHERE bots.room_id = ? ORDER BY bots.id"

        /** Where it stands, how it looks and how it behaves; owner, kind, effect and bubble never change in a room. */
        const val UPDATE_SQL =
            "UPDATE bots SET room_id = ?, x = ?, y = ?, z = ROUND(?, 1), direction = ?, dance = ?, " +
                "name = ?, motto = ?, figure = ?, gender = ?, " +
                "roams = ?, chat_auto = ?, chat_random = ?, chat_delay_seconds = ? WHERE id = ?"
    }
}
