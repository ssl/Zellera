package zel.room.furni.buildpass

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.protocol.generated.BuildPassPlacementWarningOutCodec
import zel.protocol.generated.ItemAddOutCodec
import zel.protocol.generated.ObjectAddOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.PlacementKind
import zel.room.Room
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.furni.BuildPass
import zel.room.furni.FurniBehaviours
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.place.FloorLanding
import zel.room.furni.place.FurniOut
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.furni.place.PlaceError
import zel.room.furni.place.Placeable
import zel.room.furni.place.PlacedCount
import zel.room.furni.place.RoomLimits
import zel.room.furni.place.UnitsOnTiles
import zel.room.furni.place.ViewerStates
import zel.room.furni.toSave
import zel.room.map.Point
import zel.room.persist.FurniData
import zel.room.tick.Audience
import zel.room.tick.Lane

class BuildPassPlacing(
    private val store: BuildPassStore,
    private val saves: FurniData,
    private val writes: RoomWrites,
    private val behaviours: FurniBehaviours,
    private val isStaff: (Int) -> Boolean = { false },
    private val limits: RoomLimits = RoomLimits(),
    private val units: UnitsOnTiles = UnitsOnTiles.None,
    private val viewerStates: ViewerStates = ViewerStates.None,
    private val placedCount: PlacedCount = PlacedCount.None,
    private val placedByPlayer: (Int) -> Unit = {},
) {
    private val lock = BuildPassRoomLock(store, writes)

    data class Ready(
        val hidesRoom: Boolean,
    )

    fun check(
        scope: RoomScope,
        placer: BuildPassPlacer,
        request: BuildPassRequest,
        offer: BuildPassOffer?,
    ): Ready? {
        val room = scope.room
        val player = placer.access.playerId
        val pool = placer.pool
        val visitor = !placer.access.mayPlace && !placer.access.hasRights && room.info.groupId <= 0
        if (visitor && room.units.ofPlayer(player) != null) return null
        // The placer first, then what is placed, then whose furni pool pays for it.
        val refusal =
            when {
                room.units.ofPlayer(player) == null -> PlaceError.NotOwner
                offer == null || !onFloor(room, request.spot) -> PlaceError.CantSetItem
                offer.clubOnly && !placer.hasClub -> PlaceError.NotOwner
                pool == null -> return notAdmin(scope, placer)
                pool.isFull -> PlaceError.MaxFurniture
                rules.nonMemberAloneOnly && !placer.own.isMember && hasVisitors(room, player) -> PlaceError.NotOwner
                else -> null
            }
        if (refusal != null || offer == null) return refuse(scope, player, refusal ?: PlaceError.CantSetItem)
        val hides =
            rules.roomLock && locksWith(room, placer) && !placer.roomOwnerIsMember && !room.info.isBuildPassLocked
        if (hides && rules.placementWarning && !request.confirmed) {
            warn(scope, player, request)
            return null
        }
        limits.check(room, offer.definition)?.let { return refuse(scope, player, it) }
        return Ready(hides)
    }

    fun place(
        scope: RoomScope,
        placer: BuildPassPlacer,
        request: BuildPassRequest,
        offer: BuildPassOffer,
        row: BuildPassRow,
        ready: Ready,
    ) {
        val player = placer.access.playerId
        counts(scope, placer, placed = 1)
        val spot = request.spot
        val furni =
            when (spot) {
                is BuildPassSpot.Floor -> placeFloor(scope, placer, offer, row, spot, ready)
                is BuildPassSpot.Wall -> placeWall(scope, placer, offer, row, spot, ready)
            }
        if (furni == null) {
            counts(scope, placer, placed = 0)
            writes.write(scope.room.info.id, Runnable { store.delete(row.rowId) })
            FurniOut.error(scope, player, PlaceError.CantSetItem)
            return
        }
        write(scope.room, furni)
        placedByPlayer(player)
        placedCount.anyPlaced(scope, player)
        if (scope.room.info.ownerId == player) placedCount.placed(scope, player)
    }

    private fun placeFloor(
        scope: RoomScope,
        placer: BuildPassPlacer,
        offer: BuildPassOffer,
        row: BuildPassRow,
        spot: BuildPassSpot.Floor,
        ready: Ready,
    ): RoomFurni? {
        val room = scope.room
        val direction = offer.definition.shownDirection(spot.direction)
        val landing = FloorLanding.place(room, offer.definition, spot.tile, direction)
        if (landing !is FloorLanding.Result.Lands) return null
        follow(scope, placer)
        val heights = HeightMapChange.of(room, landing.tiles)
        val furni = furniOf(placer, offer, row, spot.tile, landing.z, direction, null)
        room.furni.add(furni)
        if (viewerStates.shows(scope, furni)) {
            scope.out.send(Lane.Now, Audience.Room, ObjectAddOutCodec, FurniWire.objectAdd(furni))
        }
        FurniOut.heights(scope, heights)
        if (Interactions.changesViewerStates(furni.definition)) viewerStates.resend(scope)
        (furni.behaviour as? Placeable)?.onPlaced(scope, furni)
        units.furniChanged(scope, emptyList(), landing.tiles)
        if (ready.hidesRoom) BuildPassOut.roomSettings(scope)
        return furni
    }

    private fun placeWall(
        scope: RoomScope,
        placer: BuildPassPlacer,
        offer: BuildPassOffer,
        row: BuildPassRow,
        spot: BuildPassSpot.Wall,
        ready: Ready,
    ): RoomFurni {
        follow(scope, placer)
        val furni = furniOf(placer, offer, row, null, 0.0, 0, spot.position)
        scope.room.furni.add(furni)
        scope.out.send(Lane.Now, Audience.Room, ItemAddOutCodec, FurniWire.itemAdd(furni))
        (furni.behaviour as? Placeable)?.onPlaced(scope, furni)
        if (ready.hidesRoom) BuildPassOut.roomSettings(scope)
        return furni
    }

    /** The pool's count goes out before the furni, already holding the [placed] one. */
    private fun counts(
        scope: RoomScope,
        placer: BuildPassPlacer,
        placed: Int,
    ) {
        placer.pool?.let { BuildPassOut.count(scope, it.playerId, it.furniCount + placed) }
        placer.admins
            .filter { scope.room.units.ofPlayer(it.playerId) != null }
            .forEach { BuildPassOut.count(scope, it.playerId, it.furniCount) }
    }

    /** Before the furni goes in, the room follows the lock rule as if it stood there already. */
    private fun follow(
        scope: RoomScope,
        placer: BuildPassPlacer,
    ) {
        if (!locksWith(scope.room, placer)) return
        if (lock.sync(scope, placer.roomOwnerIsMember, placing = true) != BuildPassLockChange.Locked) return
        BuildPassOut.notice(scope, placer.access.playerId, ROOM_LOCKED)
        BuildPassOut.roomSettings(scope)
    }

    private fun notAdmin(
        scope: RoomScope,
        placer: BuildPassPlacer,
    ): Ready? {
        val player = placer.access.playerId
        FurniOut.dialog(scope, player, NOT_GROUP_ADMIN)
        BuildPassOut.count(scope, player, placer.own.furniCount)
        BuildPassOut.status(scope, placer.own)
        return null
    }

    private fun refuse(
        scope: RoomScope,
        player: Int,
        error: PlaceError,
    ): Ready? {
        FurniOut.error(scope, player, error)
        return null
    }

    private fun warn(
        scope: RoomScope,
        player: Int,
        request: BuildPassRequest,
    ) {
        val spot = request.spot
        val floor = spot as? BuildPassSpot.Floor
        scope.out.send(
            Lane.Now,
            Audience.Player(player),
            BuildPassPlacementWarningOutCodec,
            CatalogMessages.placementWarning(
                if (floor == null) PlacementKind.WALL else PlacementKind.FLOOR,
                request.pageId,
                request.offerId,
                request.sent,
                floor?.tile?.x ?: 0,
                floor?.tile?.y ?: 0,
                floor?.direction ?: 0,
                (spot as? BuildPassSpot.Wall)?.position,
            ),
        )
    }

    /** Only furni from the owner's pool follow the owner's membership; staff furni there neither warn nor lock. */
    private fun locksWith(
        room: Room,
        placer: BuildPassPlacer,
    ): Boolean = placer.pool?.playerId == room.info.ownerId

    private fun hasVisitors(
        room: Room,
        player: Int,
    ): Boolean = room.units.playerIds().any { it != player && !isStaff(it) }

    private fun onFloor(
        room: Room,
        spot: BuildPassSpot,
    ): Boolean = spot !is BuildPassSpot.Floor || room.floor.isFloor(spot.tile)

    @Suppress("LongParameterList") // one parameter per placed column
    private fun furniOf(
        placer: BuildPassPlacer,
        offer: BuildPassOffer,
        row: BuildPassRow,
        anchor: Point?,
        z: Double,
        direction: Int,
        wallPosition: String?,
    ): RoomFurni =
        RoomFurni(
            id = row.clientId,
            ownerId = BuildPass.OWNER_ID,
            ownerName = BuildPass.ownerName,
            definition = offer.definition,
            behaviour = behaviours.forType(Interactions.behaviourKey(offer.definition)),
            anchor = anchor,
            z = z,
            direction = direction,
            state = offer.state,
            wallPosition = wallPosition,
            groupId = 0,
            limitedData = NOT_LIMITED,
            rowId = row.rowId,
            placerId = placer.pool?.playerId ?: placer.access.playerId,
            placerName = placer.poolName,
        )

    @Suppress("TooGenericExceptionCaught") // logged; the room keeps the furni
    private fun write(
        room: Room,
        furni: RoomFurni,
    ) {
        val roomId = room.info.id
        val save = furni.toSave()
        writes.write(
            roomId,
            Runnable {
                try {
                    saves.save(roomId, listOf(save))
                } catch (e: Exception) {
                    log.error("room {}: placing furni {} failed to save", roomId, furni.id, e)
                }
            },
        )
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(BuildPassPlacing::class.java)

        const val ROOM_LOCKED = "build_pass.room_locked"
        const val NOT_GROUP_ADMIN = "builder.placement_widget.error.not_group_admin"
        const val NOT_LIMITED = "0:0"
    }
}
