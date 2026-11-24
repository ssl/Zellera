package zel.room.games

import zel.protocol.generated.LookToInCodec
import zel.protocol.generated.SetClothingChangeDataInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.map.Point
import zel.room.unit.walk.Walking

class GamePackets(
    private val rooms: RoomRegistry,
    private val part: GamesPart,
) : PacketPackage {
    override val name = "room-games"
    override val routes =
        listOf(
            PacketRoute(SetClothingChangeDataInCodec) { session, m ->
                val playerId = playerOf(session)
                val roomId = playerId?.let(rooms::roomOf)
                if (roomId != null) {
                    rooms.withRoom(roomId) { scope ->
                        part.saveGateOutfit(scope, playerId, m.itemId, m.gender, m.look)
                    }
                }
            },
        )

    private fun playerOf(session: PacketSession): Int? =
        session.client.player
            ?.profile
            ?.id

    fun taggingLookTo(
        units: PacketPackage,
        walking: Walking,
    ): PacketPackage =
        object : PacketPackage {
            override val name = units.name
            override val routes =
                units.routes.filterNot { it.codec === LookToInCodec } +
                    PacketRoute(LookToInCodec) { session, m ->
                        val playerId = playerOf(session)
                        val roomId = playerId?.let(rooms::roomOf)
                        if (roomId != null) {
                            rooms.withRoom(roomId) { scope ->
                                scope.room.units.ofPlayer(playerId)?.let { unit ->
                                    val point = Point(m.x, m.y)
                                    walking.lookTo(unit, point)
                                    if (!unit.isWalking) unit.changed = true
                                    part.lookedAt(scope, unit, point)
                                }
                            }
                        }
                    }
        }
}
