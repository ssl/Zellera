package zel.game.commands

import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.room.port.RoomCommands

/** Staff commands on a player in the speaker's room; a missing, protected or out-of-reach target ends them quietly. */
class StaffCommands(
    private val targets: StaffRoomTarget,
    private val rooms: () -> RoomCommands,
) {
    fun all(): List<ChatCommand> =
        listOf(
            onTarget(listOf("kick"), Privilege.COMMAND_KICK, Privilege.ROOM_KICK_IMMUNE) { staff, target ->
                rooms().staffKick(staff.profile.id, target.profile.id)
            },
            onTarget(listOf("mute", "shutup"), Privilege.COMMAND_MUTE, Privilege.CHAT_MUTE_IMMUNE) { staff, target ->
                rooms().staffMute(staff.profile.id, target.profile.id, MUTE_MINUTES)
            },
        )

    private fun onTarget(
        words: List<String>,
        needs: Privilege,
        shield: Privilege,
        act: (Player, Player) -> Unit,
    ) = ChatCommand(words, needs) { said ->
        val target = said.args.firstOrNull()?.let { targets.find(said.player, it) }
        if (target != null && !target.hasPermission(shield)) act(said.player, target)
        true
    }

    internal companion object {
        /** The shortest mute the room's own player menu offers. */
        const val MUTE_MINUTES = 2
    }
}
