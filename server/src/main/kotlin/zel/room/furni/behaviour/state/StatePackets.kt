package zel.room.furni.behaviour.state

import zel.game.quests.FurniAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.FurniRecolorInCodec
import zel.protocol.generated.SetRandomStateInCodec
import zel.protocol.generated.UseWallItemInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.furni.Usable
import zel.room.unit.RoomUnit

class StatePackets(
    private val registry: RoomRegistry,
    part: StatePart,
) : PacketPackage {
    private val recolors = Recolors(part)

    override val name = "furni-state"
    override val routes =
        listOf(
            PacketRoute(UseWallItemInCodec) { session, m ->
                withAvatar(session) { scope, _, playerId ->
                    val furni = scope.room.furni[m.itemId]
                    if (furni != null &&
                        furni.isWall
                    ) {
                        PlayerActions.touchedFurni(session.client.player, FurniAction.USE, furni.definition.name)
                        (furni.behaviour as? Usable)?.onUse(scope, furni, playerId, m.state)
                    }
                }
            },
            PacketRoute(SetRandomStateInCodec) { session, m ->
                withAvatar(session) { scope, _, playerId ->
                    val furni = scope.room.furni[m.itemId]
                    if (furni != null) (furni.behaviour as? RandomState)?.draw(scope, furni, playerId)
                }
            },
            PacketRoute(FurniRecolorInCodec) { session, m ->
                withAvatar(session) { scope, _, playerId ->
                    recolors.recolor(scope, playerId, m.furniId, m.scope, m.red, m.green, m.blue)
                }
            },
        )

    private fun withAvatar(
        session: PacketSession,
        block: (RoomScope, RoomUnit, Int) -> Unit,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        registry.withPlayer(playerId) { scope, unit -> block(scope, unit, playerId) }
    }
}
