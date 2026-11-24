package zel.room.port

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.write
import javax.sql.DataSource

class GroupRoomStore(
    private val dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    /** The room joins the group and loses its rights holders in one transaction. */
    fun assign(
        roomId: Int,
        groupId: Int,
    ) {
        dataSource.connection.use { c ->
            c.allOrNothing {
                c.write(SET_GROUP) {
                    int(groupId)
                    int(roomId)
                }
                c.write(DROP_RIGHTS) { int(roomId) }
            }
        }
    }

    fun clearGroup(roomId: Int) {
        sql.write(CLEAR_GROUP, roomId)
    }

    fun staffPick(
        roomId: Int,
        picked: Boolean,
    ) {
        sql.write(SET_STAFF_PICK, picked, roomId)
    }

    fun categoryAndTrade(
        roomId: Int,
        categoryId: Int,
        tradeMode: Int,
    ) {
        sql.write(SET_CATEGORY_TRADE, categoryId, tradeMode, roomId)
    }

    fun moderate(
        roomId: Int,
        newName: String?,
        lock: Boolean,
    ) {
        if (newName != null) sql.write(SET_NAME, newName, roomId)
        if (lock) sql.write(SET_LOCKED, roomId)
    }

    private companion object {
        const val SET_STAFF_PICK = "UPDATE rooms SET is_staff_picked = ? WHERE id = ?"
        const val SET_CATEGORY_TRADE = "UPDATE rooms SET category_id = ?, trade_mode = ? WHERE id = ?"
        const val SET_NAME = "UPDATE rooms SET name = ? WHERE id = ?"
        const val SET_LOCKED = "UPDATE rooms SET door_mode = 'locked' WHERE id = ?"
        const val SET_GROUP = "UPDATE rooms SET guild_id = ? WHERE id = ?"
        const val CLEAR_GROUP = "UPDATE rooms SET guild_id = NULL WHERE id = ?"
        const val DROP_RIGHTS = "DELETE FROM room_rights WHERE room_id = ?"
    }
}
