package zel.game.navigation

import java.sql.ResultSet
import java.util.concurrent.CopyOnWriteArrayList

class PublicRoomFolder(
    row: ResultSet,
) {
    val id: Int = row.getInt("id")
    val name: String = row.getString("name").orEmpty()
    val listMode: BlockStyle = if (row.getBoolean("show_image")) BlockStyle.TILES else BlockStyle.ROWS
    val order: Int = row.getInt("sort_order")

    private val listed = CopyOnWriteArrayList<Int>()

    val roomIds: List<Int> get() = listed.toList()

    fun add(roomId: Int) {
        listed.addIfAbsent(roomId)
    }

    fun remove(roomId: Int) {
        listed.remove(roomId)
    }
}
