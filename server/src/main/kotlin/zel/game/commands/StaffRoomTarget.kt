package zel.game.commands

import zel.game.permissions.Permissions
import zel.game.players.Player
import zel.room.port.RoomCommands

class StaffRoomTarget(
    private val permissions: Permissions,
    private val rooms: () -> RoomCommands,
    private val player: (Int) -> Player?,
) {
    fun find(
        staff: Player,
        name: String,
    ): Player? {
        val id = rooms().playerNamed(staff.profile.id, name) ?: return null
        return player(id)?.takeIf { reaches(staff, it) }
    }

    internal fun reaches(
        staff: Player,
        target: Player,
    ): Boolean =
        staff.profile.id != target.profile.id && permissions.outranks(staff.profile.rank.id, target.profile.rank.id)
}
