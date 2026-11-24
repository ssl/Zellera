package zel.room.furni.place

import zel.db.Sql
import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.room.RoomInfo
import javax.sql.DataSource

/** Read before the room lock is taken. */
class StoredFurniAccess(
    private val dataSource: DataSource,
    private val groupDecorates: (Int, Int) -> Boolean = { _, _ -> false },
) {
    fun of(
        player: Player,
        room: RoomInfo,
    ): FurniAccess {
        val id = player.profile.id
        val owner = room.ownerId == id || player.isAnyOwner
        return FurniAccess(
            playerId = id,
            isRoomOwner = owner,
            hasRights = !owner && hasRights(room.id, id),
            groupDecorates = !owner && room.groupId > 0 && groupDecorates(room.groupId, id),
            staffPlaces = player.hasPermission(Privilege.ROOM_PLACE_ANYWHERE),
            staffMoves = player.hasPermission(Privilege.ROOM_MOVE_ANYWHERE),
        )
    }

    private fun hasRights(
        roomId: Int,
        playerId: Int,
    ): Boolean =
        Sql(dataSource)
            .select(RIGHTS) {
                int(roomId)
                int(playerId)
            }.first { true } ?: false

    private companion object {
        const val RIGHTS = "SELECT 1 FROM room_rights WHERE room_id = ? AND player_id = ? LIMIT 1"
    }
}
