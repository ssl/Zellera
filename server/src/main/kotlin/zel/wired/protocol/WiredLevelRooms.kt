package zel.wired.protocol

import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.RoomScope

/** Runs [block] in the sender's room with their wired level there; a sender without a level is ignored. */
fun RoomRegistry.withWiredLevel(
    session: PacketSession,
    levels: (Room) -> Map<Int, Int>,
    block: (RoomScope, Int, Int) -> Unit,
) {
    val playerId =
        session.client.player
            ?.profile
            ?.id ?: return
    val roomId = roomOf(playerId) ?: return
    withRoom(roomId) { scope ->
        levels(scope.room)[playerId]?.let { block(scope, playerId, it) }
    }
}
