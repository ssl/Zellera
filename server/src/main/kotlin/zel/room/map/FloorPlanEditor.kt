package zel.room.map

import zel.protocol.generated.BroadcastOut
import zel.protocol.generated.BroadcastOutCodec
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.generated.RoomEntryTileOutCodec
import zel.protocol.generated.RoomOccupiedTilesOutCodec
import zel.protocol.generated.RoomVisualizationSettingsOutCodec
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane

class FloorPlanEditor(
    private val rooms: RoomRegistry,
    private val data: FloorData,
    private val limits: () -> FloorPlanLimits = { FloorPlanLimits() },
    private val occupied: (Room) -> List<Point> = { Footprint.occupied(it.furni.all()) },
    private val emptyRoom: (RoomScope, Int) -> List<Int> = { _, _ -> emptyList() },
    private val rebuilt: (Int, Int, List<Int>) -> Unit = { _, _, _ -> },
) {
    fun entryTile(playerId: Int) {
        inRoomOf(playerId) { scope ->
            val to = Audience.Player(playerId)
            scope.out.send(Lane.Now, to, RoomEntryTileOutCodec, MapMessages.entryTile(scope.room.floor.door))
            val visuals = MapMessages.visualization(scope.room.info.visuals)
            scope.out.send(Lane.Now, to, RoomVisualizationSettingsOutCodec, visuals)
        }
    }

    fun occupiedTiles(playerId: Int) {
        inRoomOf(playerId) { scope ->
            val tiles = occupied(scope.room)
            val message = MapMessages.occupiedTiles(tiles)
            scope.out.send(Lane.Now, Audience.Player(playerId), RoomOccupiedTilesOutCodec, message)
        }
    }

    fun save(
        player: FloorPlanPlayer,
        body: ByteArray,
    ) {
        val roomId = rooms.roomOf(player.playerId) ?: return
        val accepted = rooms.withRoom(roomId) { scope -> checkSave(scope, player, body) } ?: return
        val room = rooms.loaded(roomId) ?: return
        val visuals =
            room.info.visuals.copy(wallThickness = accepted.wallThickness, floorThickness = accepted.floorThickness)
        val floor = CustomFloor(FloorMapText.store(accepted.grid), accepted.door)
        data.saveFloorPlan(roomId, floor, visuals, accepted.fixedWallHeight)
        val waiting = rooms.withRoom(roomId) { scope -> emptyRoom(scope, player.playerId) } ?: return
        rooms.unload(roomId)
        rebuilt(player.playerId, roomId, waiting)
    }

    private fun checkSave(
        scope: RoomScope,
        player: FloorPlanPlayer,
        body: ByteArray,
    ): FloorPlanOutcome.Accepted? {
        val room = scope.room
        val to = Audience.Player(player.playerId)
        if (room.info.ownerId != player.playerId && !player.anyRoomOwner) return null
        if (!player.mayEdit) {
            val text = player.noPermissionText ?: return null
            scope.out.send(Lane.Now, to, BroadcastOutCodec, BroadcastOut(text))
            return null
        }
        val request = FloorPlanRequest.read(body) ?: return null
        return when (val outcome = FloorPlanCheck.check(request, room.floor, occupied(room), limits = limits())) {
            FloorPlanOutcome.Dropped -> {
                null
            }

            is FloorPlanOutcome.Refused -> {
                scope.out.send(Lane.Now, to, NotificationDialogOutCodec, errorDialog(outcome.errors))
                null
            }

            is FloorPlanOutcome.Accepted -> {
                outcome
            }
        }
    }

    private fun inRoomOf(
        playerId: Int,
        block: (RoomScope) -> Unit,
    ) {
        val roomId = rooms.roomOf(playerId) ?: return
        rooms.withRoom(roomId, block)
    }

    companion object {
        const val ERROR_DIALOG = "floorplan_editor.error"
        const val ERRORS_PARAM = "errors"
        private const val LINE_BREAK = "<br />"

        fun errorDialog(errors: List<FloorPlanError>): NotificationDialogOut {
            val text =
                errors.joinToString(LINE_BREAK) { error ->
                    val where = error.tile?.let { "${it.x},${it.y}" } ?: "%%%general%%%"
                    val limit = error.maxTiles?.let { " (%%%max%%% $it %%%tiles%%%)" }.orEmpty()
                    "($where): %%%${error.key}%%%$limit"
                }
            val params = listOf(NotificationDialogOut.List1Item(ERRORS_PARAM, text))
            return NotificationDialogOut(ERROR_DIALOG, params)
        }
    }
}
