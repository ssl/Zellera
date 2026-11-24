package zel.room.protocol

import zel.core.rules.rules
import zel.game.players.Player
import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.MoveObjectInCodec
import zel.protocol.generated.MoveWallItemInCodec
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.PickupObjectInCodec
import zel.protocol.generated.PlaceObjectInCodec
import zel.protocol.generated.PlacePostItInCodec
import zel.protocol.generated.SetAdjacentCustomStackingHeightInCodec
import zel.protocol.generated.SetCustomStackingHeightInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.furni.place.FurniAccess
import zel.room.furni.place.FurniMoving
import zel.room.furni.place.FurniPickup
import zel.room.furni.place.FurniPlacing
import zel.room.furni.place.Placement
import zel.room.furni.place.StackHelperControl
import zel.room.furni.place.StoredFurniAccess
import zel.room.map.Point

class FurniPackets(
    private val registry: RoomRegistry,
    private val access: StoredFurniAccess,
    placing: FurniPlacing,
    moving: FurniMoving,
    pickup: FurniPickup,
    helpers: StackHelperControl,
) : PacketPackage {
    private val moveGap = ClientCooldown(MOVE_GROUP, rules.rates.furniMoveGapMillis)

    override val name = "furni"
    override val routes =
        listOf(
            PacketRoute(PlaceObjectInCodec) { session, m ->
                val placement = Placement.parse(m.placement) ?: return@PacketRoute
                inRoom(session) { scope, who -> placing.place(scope, who, placement) }
            },
            PacketRoute(PlacePostItInCodec) { session, m ->
                inRoom(session) { scope, who -> placing.placeNote(scope, who, m.itemId, m.location) }
            },
            PacketRoute(MoveObjectInCodec) { session, m ->
                if (!moveGap.acquire(session.client)) return@PacketRoute
                inRoom(session) { scope, who -> moving.move(scope, who, m.furniId, Point(m.x, m.y), m.rotation) }
            },
            PacketRoute(MoveWallItemInCodec) { session, m ->
                inRoom(session) { scope, who -> moving.moveWall(scope, who, m.itemId, m.wallPosition) }
            },
            PacketRoute(PickupObjectInCodec) { session, m ->
                val before = pickup.before(m.itemId)
                inRoom(session) { scope, who -> pickup.pickup(scope, who, m.category, m.itemId, m.confirmed, before) }
            },
            PacketRoute(CustomStackingHeightCodec) { session, m ->
                inRoom(session) { scope, who -> helpers.slider(scope, who, m.furniId, m.hundredths, m.multiWalk) }
            },
            PacketRoute(SetAdjacentCustomStackingHeightInCodec) { session, m ->
                inRoom(session) { scope, who -> helpers.arrow(scope, who, m.itemId, m.moveDown) }
            },
        )

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, FurniAccess) -> Unit,
    ) {
        val player: Player = session.client.player ?: return
        val roomId = registry.roomOf(player.profile.id) ?: return
        val info = registry.loaded(roomId)?.info ?: return
        val who = access.of(player, info)
        registry.withRoom(roomId) { scope -> block(scope, who) }
    }

    data class CustomStackingHeight(
        val furniId: Int,
        val hundredths: Int,
        val multiWalk: Boolean?,
    )

    object CustomStackingHeightCodec : IncomingCodec<CustomStackingHeight> {
        override val name = SetCustomStackingHeightInCodec.name

        override fun read(r: PacketReader): CustomStackingHeight =
            CustomStackingHeight(r.int(), r.int(), if (r.remaining > 0) r.boolean() else null)

        override fun write(
            w: PacketWriter,
            m: CustomStackingHeight,
        ) {
            w.int(m.furniId).int(m.hundredths)
            m.multiWalk?.let { w.boolean(it) }
        }
    }

    private companion object {
        const val MOVE_GROUP = "furni.move"
    }
}
