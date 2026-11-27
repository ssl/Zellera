package zel.room.furni.place

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.items.ProductKind
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.FurniListRemoveOut
import zel.protocol.generated.FurniListRemoveOutCodec
import zel.protocol.generated.ItemAddOutCodec
import zel.protocol.generated.ObjectAddOutCodec
import zel.protocol.generated.OpenOut
import zel.protocol.generated.OpenOutCodec
import zel.protocol.generated.RequestSpamWallPostItOut
import zel.protocol.generated.RequestSpamWallPostItOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.entry.DoorCheck
import zel.room.furni.FurniBehaviours
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.toSave
import zel.room.map.Point
import zel.room.persist.FurniTransfers
import zel.room.tick.Audience
import zel.room.tick.Lane

/** Runs under the room lock; the row is written on the room's [writes]. */
class FurniPlacing(
    private val inventory: FurniInventory,
    private val transfers: FurniTransfers,
    private val writes: RoomWrites,
    private val behaviours: FurniBehaviours,
    private val limits: RoomLimits = RoomLimits(),
    private val units: UnitsOnTiles = UnitsOnTiles.None,
    private val viewerStates: ViewerStates = ViewerStates.None,
    private val placedCount: PlacedCount = PlacedCount.None,
    private val rentedBy: (Room, Int, Point) -> Boolean = { _, _, _ -> false },
    private val paint: (RoomScope, playerId: Int, level: Int, itemId: Int) -> Unit = { _, _, _, _ -> },
    private val unplaced: (roomId: Int, RoomFurni, toOwner: Boolean) -> Unit = { _, _, _ -> },
) {
    fun place(
        scope: RoomScope,
        access: FurniAccess,
        placement: Placement,
    ) {
        val room = scope.room
        val player = access.playerId
        if (room.units.ofPlayer(player) == null) return FurniOut.error(scope, player, PlaceError.NotOwner)
        if (!access.mayPlace && !(placement is Placement.Floor && rentedBy(room, player, placement.tile))) {
            return FurniOut.error(scope, player, PlaceError.NotOwner)
        }
        val item = inventory.find(player, placement.itemId) ?: return
        if (Interactions.of(item.definition) == Interactions.POST_IT) return
        if (Interactions.of(item.definition) == ROOM_EFFECT || item.definition.name in DECORATIONS) {
            paint(scope, player, if (access.isRoomOwner) DoorCheck.OWNER else 0, item.id)
            return
        }
        when (placement) {
            is Placement.Floor -> placeFloor(scope, access, item, placement)
            is Placement.Wall -> placeWall(scope, access, item, placement.position)
        }
    }

    fun placeNote(
        scope: RoomScope,
        access: FurniAccess,
        itemId: Int,
        position: String,
    ) {
        val room = scope.room
        val player = access.playerId
        if (!WallPosition.isValid(position) || room.units.ofPlayer(player) == null) return
        val item = inventory.find(player, itemId) ?: return
        if (Interactions.of(item.definition) != Interactions.POST_IT) return
        val pole = room.furni.all().any { Interactions.of(it.definition) == Interactions.STICKY_POLE }
        if (!access.mayPostNotes && !pole) return
        if (!limits.notesLeft(room)) return FurniOut.error(scope, player, PlaceError.MaxStickies)
        if (!inventory.take(player, itemId)) return
        val state = item.state.takeUnless { it.isEmpty() || it == "0" } ?: NOTE_COLOUR
        // As in Habbo, a note becomes the room owner's whoever sticks it up; an ownerless room leaves it the placer's.
        val keeper = room.info.takeIf { it.ownerId > 0 }
        val ownerId = keeper?.ownerId ?: item.ownerId
        val furni = furniOf(item, null, 0.0, 0, state, position, ownerId, keeper?.ownerName ?: item.ownerName)
        room.furni.add(furni)
        scope.out.send(Lane.Now, Audience.Room, ItemAddOutCodec, FurniWire.itemAdd(furni))
        scope.out.send(Lane.Now, Audience.Player(player), FurniListRemoveOutCodec, FurniListRemoveOut(furni.id))
        if (!access.mayPostNotes) {
            scope.out.send(
                Lane.Now,
                Audience.Player(player),
                RequestSpamWallPostItOutCodec,
                RequestSpamWallPostItOut(furni.id, position),
            )
        }
        write(room, furni)
        placedCount.noteLeft(scope, player)
    }

    /** A renter's furni lands with its whole footprint inside his space. */
    private fun refusal(
        room: Room,
        access: FurniAccess,
        landing: FloorLanding.Result,
    ): PlaceError? =
        when {
            landing !is FloorLanding.Result.Lands -> PlaceError.CantSetItem
            access.mayPlace || landing.tiles.all { rentedBy(room, access.playerId, it) } -> null
            else -> PlaceError.NotOwner
        }

    private fun placeFloor(
        scope: RoomScope,
        access: FurniAccess,
        item: InventoryItem,
        placement: Placement.Floor,
    ) {
        val room = scope.room
        val player = access.playerId
        if (item.definition.type != ProductKind.FLOOR_FURNI || !room.floor.contains(placement.tile)) return
        if (Interactions.of(item.definition) == Interactions.WEB_API && !access.isRoomOwner) {
            return FurniOut.error(scope, player, PlaceError.NotOwner)
        }
        limits.check(room, item.definition)?.let { return FurniOut.error(scope, player, it) }
        val direction = item.definition.shownDirection(placement.direction)
        val landing = FloorLanding.place(room, item.definition, placement.tile, direction)
        refusal(room, access, landing)?.let { return FurniOut.error(scope, player, it) }
        if (landing !is FloorLanding.Result.Lands || !inventory.take(player, item.id)) return
        val heights = HeightMapChange.of(room, landing.tiles)
        val furni = furniOf(item, placement.tile, landing.z, direction, item.state, null)
        (furni.behaviour as? Placeable)?.beforeAdded(furni)
        room.furni.add(furni)
        if (viewerStates.shows(scope, furni)) {
            scope.out.send(Lane.Now, Audience.Room, ObjectAddOutCodec, FurniWire.objectAdd(furni))
        }
        FurniOut.heights(scope, heights)
        if (Interactions.changesViewerStates(furni.definition)) viewerStates.resend(scope)
        (furni.behaviour as? Placeable)?.onPlaced(scope, furni)
        units.furniChanged(scope, emptyList(), landing.tiles)
        scope.out.send(Lane.Now, Audience.Player(player), FurniListRemoveOutCodec, FurniListRemoveOut(-furni.id))
        if (access.isRoomOwner && Interactions.isWiredBox(Interactions.of(furni.definition))) {
            scope.out.send(Lane.Now, Audience.Player(player), OpenOutCodec, OpenOut(furni.id))
        }
        write(room, furni)
        PlayerActions.progress(player, CountedAction.PLACE_ITEM, 1)
        placedCount.anyPlaced(scope, player)
        if (room.info.ownerId == player) placedCount.placed(scope, player)
    }

    private fun placeWall(
        scope: RoomScope,
        access: FurniAccess,
        item: InventoryItem,
        position: String,
    ) {
        val room = scope.room
        val player = access.playerId
        if (item.definition.type != ProductKind.WALL_FURNI || item.definition.name in DECORATIONS) return
        limits.check(room, item.definition)?.let { return FurniOut.error(scope, player, it) }
        if (!inventory.take(player, item.id)) return
        val furni = furniOf(item, null, 0.0, 0, item.state, position)
        room.furni.add(furni)
        scope.out.send(Lane.Now, Audience.Room, ItemAddOutCodec, FurniWire.itemAdd(furni))
        (furni.behaviour as? Placeable)?.onPlaced(scope, furni)
        scope.out.send(Lane.Now, Audience.Player(player), FurniListRemoveOutCodec, FurniListRemoveOut(furni.id))
        write(room, furni)
        PlayerActions.progress(player, CountedAction.PLACE_ITEM, 1)
        placedCount.anyPlaced(scope, player)
        if (room.info.ownerId == player) placedCount.placed(scope, player)
    }

    @Suppress("LongParameterList") // one parameter per placed column
    private fun furniOf(
        item: InventoryItem,
        anchor: Point?,
        z: Double,
        direction: Int,
        state: String,
        wallPosition: String?,
        ownerId: Int = item.ownerId,
        ownerName: String = item.ownerName,
    ): RoomFurni =
        RoomFurni(
            id = item.id,
            ownerId = ownerId,
            ownerName = ownerName,
            definition = item.definition,
            behaviour = behaviours.forType(Interactions.behaviourKey(item.definition)),
            anchor = anchor,
            z = z,
            direction = direction,
            state = state,
            wallPosition = wallPosition,
            groupId = item.groupId,
            limitedData = item.limitedData,
            placerId = item.ownerId,
            placerName = item.ownerName,
        )

    /** Moves the row from the placer's inventory into the room, as its owner's, outside the lock. */
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
                    if (!transfers.putInRoom(roomId, furni.placerId, save, furni.ownerId)) {
                        log.warn("room {}: furni {} was no longer in its placer's inventory", roomId, furni.id)
                        unplaced(roomId, furni, false)
                    }
                } catch (e: Exception) {
                    log.error("room {}: placing furni {} failed to save", roomId, furni.id, e)
                    unplaced(roomId, furni, true)
                }
            },
        )
    }

    private companion object {
        const val ROOM_EFFECT = "room.surface"
        val log: Logger = LoggerFactory.getLogger(FurniPlacing::class.java)

        const val NOTE_COLOUR = "FFFF33"

        val DECORATIONS = setOf("wallpaper", "floor", "landscape")
    }
}
