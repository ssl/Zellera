package zel.room.furni.transit

import zel.game.players.Player
import zel.game.quests.FurniAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.DiceOffInCodec
import zel.protocol.generated.EnterOneWayDoorInCodec
import zel.protocol.generated.ThrowDiceInCodec
import zel.protocol.generated.UseFurnitureInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.furni.Usable
import zel.room.furni.place.FurniAccess
import zel.room.furni.place.StoredFurniAccess
import zel.room.unit.RoomUnit

class TransitPackets(
    private val registry: RoomRegistry,
    private val access: StoredFurniAccess,
    transit: TransitPart,
) : PacketPackage {
    override val name = "transit"
    override val routes =
        listOf(
            PacketRoute(UseFurnitureInCodec) { session, m ->
                withAvatar(session) { scope, unit, who ->
                    val furni = scope.room.furni[m.itemId]
                    if (furni != null) {
                        PlayerActions.touchedFurni(session.client.player, FurniAction.USE, furni.definition.name)
                    }
                    if (!transit.use(scope, unit, m.itemId, who)) {
                        if (furni != null) (furni.behaviour as? Usable)?.onUse(scope, furni, who.playerId, m.state)
                    }
                }
            },
            PacketRoute(EnterOneWayDoorInCodec) { session, m ->
                withAvatar(session) { scope, unit, _ -> transit.enterOneWayGate(scope, unit, m.itemId) }
            },
            PacketRoute(ThrowDiceInCodec) { session, m ->
                withAvatar(session) { scope, unit, _ -> transit.rollDice(scope, unit, m.itemId) }
            },
            PacketRoute(DiceOffInCodec) { session, m ->
                withAvatar(session) { scope, unit, _ -> transit.closeDice(scope, unit, m.itemId) }
            },
        )

    /** The rights are read before the lock. */
    private fun withAvatar(
        session: PacketSession,
        block: (RoomScope, RoomUnit, FurniAccess) -> Unit,
    ) {
        val player: Player = session.client.player ?: return
        val playerId = player.profile.id
        val roomId = registry.roomOf(playerId) ?: return
        val info = registry.loaded(roomId)?.info ?: return
        val who = access.of(player, info)
        registry.withRoom(roomId) { scope ->
            scope.room.units
                .ofPlayer(playerId)
                ?.let { block(scope, it, who) }
        }
    }
}
