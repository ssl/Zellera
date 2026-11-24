package zel.game.moderation

import zel.core.Configuration
import zel.core.Texts
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.players.Players
import zel.protocol.generated.ModeratorCautionOut
import zel.protocol.generated.ModeratorCautionOutCodec
import zel.protocol.generated.ModeratorOut
import zel.protocol.generated.ModeratorOutCodec
import zel.room.listing.ListedRoom
import zel.room.port.RoomPort

/** What a moderator does to a player or a room on the spot; each answers whether it was done. */
class StaffActions(
    private val config: Configuration,
    private val texts: Texts,
    private val players: Players,
    private val reach: StaffReach,
    private val rooms: () -> RoomPort,
) {
    /** A plain message from the moderator. */
    fun message(
        moderator: Player,
        target: Player,
        text: String,
    ): Boolean {
        if (!reach.reaches(moderator, target.profile)) return false
        target.client?.send(ModeratorOutCodec, ModeratorOut(text, ""))
        return true
    }

    /** A formal warning, with the link to the rules when one is set. */
    fun caution(
        moderator: Player,
        target: Player,
        text: String,
    ): Boolean {
        if (!reach.reaches(moderator, target.profile)) return false
        target.client?.send(ModeratorCautionOutCodec, ModeratorCautionOut(text, config.getValue(CAUTION_LINK_KEY, "")))
        return true
    }

    /** Sends [target] out of the room they are in and tells them why. */
    fun kick(
        moderator: Player,
        target: Player,
        text: String,
    ): Boolean {
        val id = target.profile.id
        if (rooms().roomOf(id) == null || !mayRemove(moderator, target)) return false
        rooms().kick(id)
        target.client?.send(ModeratorOutCodec, ModeratorOut(text, ""))
        return true
    }

    /**
     * Steps into a room whose owner the moderator outranks: replaces its name, locks its door, sends out everyone the
     * moderator could send out one by one, except the owner and other holders of the tool.
     */
    fun intervene(
        room: ListedRoom,
        moderator: Player,
        measures: RoomMeasures,
    ): Boolean {
        if (!reach.canModerate(moderator, room.ownerId)) return false
        if (measures.clearName || measures.lockDoor) {
            val name = if (measures.clearName) texts.text("moderation.room.cleared_name") else null
            rooms().moderate(room.id, name, measures.lockDoor)
        }
        if (measures.emptyRoom) {
            rooms().kickAll(room.id) { playerId ->
                val player = players.find(playerId)
                player == null ||
                    playerId == room.ownerId ||
                    player.hasPermission(Privilege.MODERATION_TOOL) ||
                    !mayRemove(moderator, player)
            }
        }
        return true
    }

    private fun mayRemove(
        moderator: Player,
        target: Player,
    ): Boolean = !target.hasPermission(Privilege.ROOM_KICK_IMMUNE) && reach.reaches(moderator, target.profile)

    data class RoomMeasures(
        val emptyRoom: Boolean,
        val lockDoor: Boolean,
        val clearName: Boolean,
    )

    private companion object {
        const val CAUTION_LINK_KEY = "moderation.caution.link"
    }
}
