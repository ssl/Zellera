package zel.room.persist

import zel.db.allOrNothing
import zel.db.first
import zel.db.write
import javax.sql.DataSource

class RoomVotes(
    private val dataSource: DataSource,
) {
    fun vote(
        roomId: Int,
        playerId: Int,
    ): Int =
        dataSource.connection.use { c ->
            c.allOrNothing {
                val counted =
                    c.write(ADD_VOTE) {
                        int(playerId)
                        int(roomId)
                    } == 1
                if (counted) c.write(ADD_SCORE) { int(roomId) }
                c.first(SCORE, { int(roomId) }) { it.getInt(1) } ?: 0
            } ?: 0
        }

    private companion object {
        const val ADD_VOTE = "INSERT IGNORE INTO room_votes (player_id, room_id) VALUES (?, ?)"
        const val ADD_SCORE = "UPDATE rooms SET score = score + 1 WHERE id = ?"
        const val SCORE = "SELECT score FROM rooms WHERE id = ?"
    }
}
