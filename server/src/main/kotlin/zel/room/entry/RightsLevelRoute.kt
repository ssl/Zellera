package zel.room.entry

import zel.game.players.Player
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope

/** Runs [block] in the sender's room with the player and the rights level they entered with. */
fun RoomRegistry.withRightsLevel(
    session: PacketSession,
    leaving: RoomLeaving,
    block: (RoomScope, Player, Int) -> Unit,
) {
    val player = session.client.player ?: return
    val playerId = player.profile.id
    withPlayer(playerId) { scope, _ -> block(scope, player, leaving.doorOf(scope.room).levelOf(playerId)) }
}
