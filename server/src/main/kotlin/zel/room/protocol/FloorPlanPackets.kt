package zel.room.protocol

import zel.core.rules.FloorPlanAccess
import zel.core.rules.rules
import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.game.players.subscriptions.Subscription
import zel.protocol.generated.GetOccupiedTilesInCodec
import zel.protocol.generated.GetRoomEntryTileInCodec
import zel.protocol.generated.UpdateFloorPropertiesInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.map.FloorPlanEditor
import zel.room.map.FloorPlanPlayer

class FloorPlanPackets(
    editor: FloorPlanEditor,
) : PacketPackage {
    override val name = "floorplan"
    override val routes =
        listOf(
            PacketRoute(GetRoomEntryTileInCodec) { session, _ ->
                session.player()?.let { editor.entryTile(it.profile.id) }
            },
            PacketRoute(GetOccupiedTilesInCodec) { session, _ ->
                session.player()?.let { editor.occupiedTiles(it.profile.id) }
            },
            PacketRoute(UpdateFloorPropertiesInCodec) { session, message ->
                session.player()?.let { editor.save(floorPlanPlayer(it), message.rest) }
            },
        )

    private fun PacketSession.player(): Player? = client.player

    private fun floorPlanPlayer(player: Player): FloorPlanPlayer =
        FloorPlanPlayer(
            playerId = player.profile.id,
            mayEdit =
                when (rules.floorPlanEditor) {
                    FloorPlanAccess.PERMISSION -> player.hasPermission(Privilege.ROOM_FLOOR_PLAN)
                    FloorPlanAccess.BUILD_PASS_MEMBER -> player.stats.club.has(Subscription.BUILD_PASS)
                    FloorPlanAccess.OWNER -> true
                },
            anyRoomOwner = player.isAnyOwner,
            noPermissionText =
                player.services.texts
                    .text(NO_PERMISSION_TEXT, "")
                    .takeIf { rules.floorPlanEditor == FloorPlanAccess.PERMISSION },
        )

    private companion object {
        const val NO_PERMISSION_TEXT = "room.floor_plan.not_allowed"
    }
}
