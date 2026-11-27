package zel.wired.trigger

import zel.game.treasurehunt.TreasureHunts
import zel.protocol.generated.ClickFurniInCodec
import zel.protocol.generated.WiredClickPlayerInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.unit.RoomUnit
import zel.room.unit.walk.Walking
import zel.wired.BoxSettingsSource
import kotlin.math.absoluteValue

class TriggerPackets(
    private val rooms: RoomRegistry,
    forms: BoxSettingsSource,
    walking: Walking,
) : PacketPackage {
    private val users = ClickUsers(walking::turnToClicked, forms)

    override val name = "wired-triggers"
    override val routes =
        listOf(
            PacketRoute(ClickFurniInCodec) { session, m ->
                withAvatar(session) { scope, unit ->
                    val id = m.itemId.absoluteValue
                    if (scope.room.furni[id] != null) {
                        scope.raise(RoomEvent.ClickFurni(unit.index, id))
                        session.client.player?.takeIf { it.services.config.getBoolean(TreasureHunts.SETTING) }?.let {
                            it.services.game.treasureHunts
                                ?.takeClick(it, id)
                        }
                    }
                }
            },
            PacketRoute(WiredClickPlayerInCodec) { session, m ->
                withAvatar(session) { scope, unit -> users.clicked(scope, unit, m.roomIndex) }
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
        val roomId = rooms.roomOf(playerId) ?: return
        rooms.withRoom(roomId) { scope ->
            scope.room.units
                .ofPlayer(playerId)
                ?.let { block(scope, it) }
        }
    }
}
