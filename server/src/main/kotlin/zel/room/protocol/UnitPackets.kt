package zel.room.protocol

import zel.protocol.generated.ChangePostureInCodec
import zel.protocol.generated.LookToInCodec
import zel.protocol.generated.MovePlayerInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.walk.Walking

class UnitPackets(
    private val rooms: RoomRegistry,
    walking: Walking,
) : PacketPackage {
    override val name = "units"
    override val routes =
        listOf(
            PacketRoute(MovePlayerInCodec) { session, m ->
                withAvatar(session) { scope, unit -> walking.request(scope, unit, Point(m.x, m.y)) }
            },
            PacketRoute(ChangePostureInCodec) { session, m ->
                withAvatar(session) { scope, unit -> walking.changePosture(scope, unit, m.posture) }
            },
            PacketRoute(LookToInCodec) { session, m ->
                withAvatar(session) { _, unit -> walking.lookTo(unit, Point(m.x, m.y)) }
            },
        )

    private fun withAvatar(
        session: PacketSession,
        block: (RoomScope, RoomUnit) -> Unit,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        rooms.withPlayer(playerId, block)
    }
}
