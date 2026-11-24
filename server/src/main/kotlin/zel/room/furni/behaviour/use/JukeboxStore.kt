package zel.room.furni.behaviour.use

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.orFallback
import zel.db.write
import zel.game.items.MusicDisc
import javax.sql.DataSource

class JukeboxStore(
    private val dataSource: DataSource,
) {
    data class StoredDisc(
        val itemId: Int,
        val songId: Int,
        val ownerId: Int,
    )

    private val sql = Sql(dataSource)

    fun discs(roomId: Int): List<StoredDisc> =
        sql.select(LIST, roomId).rows { row ->
            StoredDisc(row.getInt("id"), MusicDisc.songId(row.getString("state").orEmpty()), row.getInt("player_id"))
        }

    /** False when neither happened. */
    fun added(
        roomId: Int,
        itemId: Int,
    ): Boolean =
        log.orFallback("adding disc $itemId to the playlist of room $roomId", false) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    connection.write(ADD) {
                        int(roomId)
                        int(itemId)
                    }
                    connection.write(MOVE) { int(itemId) }
                    true
                }
            } ?: false
        }

    fun removed(
        roomId: Int,
        itemIds: Collection<Int>,
    ) {
        if (itemIds.isEmpty()) return
        log.orFallback("removing ${itemIds.size} discs from the playlist of room $roomId", Unit) {
            sql.writeEach(REMOVE, itemIds) { itemId ->
                int(roomId)
                int(itemId)
            }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(JukeboxStore::class.java)
        const val LIST =
            "SELECT disc.id, disc.state, disc.player_id FROM furni disc" +
                " WHERE disc.id IN (SELECT furni_id FROM room_playlist_discs WHERE room_id = ?) ORDER BY disc.id"
        const val ADD =
            "INSERT INTO room_playlist_discs (room_id, furni_id) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE room_id = VALUES(room_id)"
        const val REMOVE = "DELETE FROM room_playlist_discs WHERE room_id = ? AND furni_id = ?"
        const val MOVE = "UPDATE furni SET room_id = NULL WHERE id = ?"
    }
}
