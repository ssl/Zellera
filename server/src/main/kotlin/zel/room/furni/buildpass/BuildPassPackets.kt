package zel.room.furni.buildpass

import zel.core.rules.rules
import zel.protocol.generated.BuildPassPlaceRoomItemInCodec
import zel.protocol.generated.BuildPassPlaceWallItemInCodec
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.furni.place.FurniOut
import zel.room.furni.place.PlaceError
import zel.room.furni.place.StoredFurniAccess
import zel.room.furni.place.WallPosition
import zel.room.map.Point

class BuildPassPackets(
    private val registry: RoomRegistry,
    private val access: StoredFurniAccess,
    private val reader: BuildPassReader,
    private val offers: BuildPassOffers,
    private val store: BuildPassStore,
    private val placing: BuildPassPlacing,
) : PacketPackage {
    private val placeGap = ClientCooldown(PLACE_GROUP, rules.rates.buildPassPlaceGapMillis)

    override val name = "build-pass"
    override val routes =
        listOf(
            PacketRoute(BuildPassPlaceRoomItemInCodec) { session, m ->
                val spot = BuildPassSpot.Floor(Point(m.x, m.y), m.rotation)
                place(session, BuildPassRequest(m.pageId, m.offerId, m.extraData, spot, m.confirmed == true))
            },
            PacketRoute(BuildPassPlaceWallItemInCodec) { session, m ->
                if (WallPosition.isValid(m.wallPosition)) {
                    val spot = BuildPassSpot.Wall(m.wallPosition)
                    place(session, BuildPassRequest(m.pageId, m.offerId, m.extraData, spot, m.confirmed == true))
                }
            },
        )

    private fun place(
        session: PacketSession,
        request: BuildPassRequest,
    ) {
        if (!placeGap.acquire(session.client)) return
        val player = session.client.player ?: return
        val profile = player.profile
        val roomId =
            registry.roomOf(profile.id)
                ?: return session.send(
                    NotificationDialogOutCodec,
                    FurniOut.dialogOf(PlaceError.NotOwner.message),
                )
        val info = registry.loaded(roomId)?.info ?: return
        val who = access.of(player, info)
        val placer = reader.placer(who, info, profile.name, player.stats.club.isMember)
        val offer = offers.find(request.pageId, request.offerId, request.sent, request.spot.type, profile.rank.id)
        val ready = registry.withRoom(roomId) { placing.check(it, placer, request, offer) } ?: return
        val pool = placer.pool ?: return
        val made = offer ?: return
        val row = store.create(pool.playerId, made.definition.id, roomId, made.state)
        if (row == null) {
            registry.withRoom(roomId) { FurniOut.error(it, who.playerId, PlaceError.CantSetItem) }
            return
        }
        registry.withRoom(roomId) { placing.place(it, placer, request, made, row, ready) } ?: store.delete(row.rowId)
    }

    private companion object {
        const val PLACE_GROUP = "build_pass.place"
    }
}
