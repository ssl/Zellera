package zel.room.chat

import zel.db.Sql
import javax.sql.DataSource

class JdbcRoomWordStore(
    private val dataSource: DataSource,
) : RoomWordStore {
    override fun load(roomId: Int): List<String> = Sql(dataSource).select(LOAD) { int(roomId) }.rows { it.getString(1) }

    override fun add(
        roomId: Int,
        word: String,
    ) {
        Sql(dataSource).write(ADD) {
            int(roomId)
            string(word)
        }
    }

    override fun remove(
        roomId: Int,
        word: String,
    ) {
        Sql(dataSource).write(REMOVE) {
            int(roomId)
            string(word)
        }
    }

    private companion object {
        const val LOAD = "SELECT word FROM room_word_filters WHERE room_id = ?"
        const val ADD = "INSERT IGNORE INTO room_word_filters (room_id, word) VALUES (?, ?)"
        const val REMOVE = "DELETE FROM room_word_filters WHERE room_id = ? AND word = ?"
    }
}
