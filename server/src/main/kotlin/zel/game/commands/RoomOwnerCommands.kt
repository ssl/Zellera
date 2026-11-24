package zel.game.commands

import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.room.entry.DoorCheck
import zel.room.port.RoomCommands

/** Commands that clear things out of the room the speaker owns; from anyone else they are swallowed unanswered. */
class RoomOwnerCommands(
    private val rooms: () -> RoomCommands,
) {
    fun all(): List<ChatCommand> =
        listOf(
            owner("pickall") { rooms().pickAll(it) },
            owner("pickallbc") { rooms().pickBuildPass(it) },
            owner("resetscores") { rooms().resetScores(it) },
            ownerOrStaff("ejectall") { rooms().ejectGuestFurni(it) },
            ownerOrStaff("ejectpets") { rooms().ejectPets(it) },
        )

    private fun owner(
        word: String,
        act: (Int) -> Unit,
    ) = ChatCommand(listOf(word)) { said ->
        val id = said.player.profile.id
        if (rooms().rightsLevel(id) >= DoorCheck.OWNER) act(id)
        true
    }

    /** Sending other people's things back is also open to staff who manage every room. */
    private fun ownerOrStaff(
        word: String,
        act: (Int) -> Unit,
    ) = ChatCommand(listOf(word)) { said ->
        val id = said.player.profile.id
        if (mayClear(said.player, rooms().rightsLevel(id))) act(id)
        true
    }

    internal companion object {
        fun mayClear(
            player: Player,
            rightsLevel: Int,
        ): Boolean = rightsLevel >= DoorCheck.OWNER || (rightsLevel > 0 && player.isAnyOwner)
    }
}
