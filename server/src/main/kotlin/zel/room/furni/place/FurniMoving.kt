package zel.room.furni.place

import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.ItemUpdateOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane

/** Runs under the room lock; a renter moves his own floor furni only within the space he rents. */
class FurniMoving(
    private val units: UnitsOnTiles = UnitsOnTiles.None,
    private val rentedBy: (Room, Int, Point) -> Boolean = { _, _, _ -> false },
) {
    fun move(
        scope: RoomScope,
        access: FurniAccess,
        furniId: Int,
        tile: Point,
        askedDirection: Int,
    ) {
        val room = scope.room
        val furni = room.furni[furniId] ?: return
        val oldAnchor = furni.anchor ?: return
        if (!room.floor.contains(tile)) return
        val oldDirection = furni.direction
        val direction = furni.definition.directionAfterMove(askedDirection, oldDirection)
        val renter = !access.mayMove && furni.ownerId == access.playerId
        if (!access.mayMove && !renter) return FurniOut.snapBack(scope, access.playerId, PlaceError.NotOwner, furni)
        val landing = FloorLanding.move(room, furni, tile, direction)
        if (landing !is FloorLanding.Result.Lands) {
            return FurniOut.snapBack(scope, access.playerId, PlaceError.CantSetItem, furni)
        }
        if (renter && !landing.tiles.all { rentedBy(room, access.playerId, it) }) {
            return FurniOut.snapBack(scope, access.playerId, PlaceError.NotOwner, furni)
        }
        val oldTiles = furni.tiles()
        val z = if (tile == oldAnchor && isTopOf(scope, furni, oldTiles)) furni.z else landing.z
        val heights = HeightMapChange.of(room, landing.tiles + oldTiles)
        room.furni.move(furni, tile, direction)
        furni.z = z
        furni.dirty = true
        scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(furni))
        FurniOut.heights(scope, heights)
        units.furniChanged(scope, oldTiles, landing.tiles)
        (furni.behaviour as? Placeable)?.onMoved(scope, furni, oldTiles)
        if (tile != oldAnchor) PlayerActions.progress(access.playerId, CountedAction.MOVE_ITEM, 1)
        if (direction != oldDirection) PlayerActions.progress(access.playerId, CountedAction.ROTATE_ITEM, 1)
    }

    /** Moves are never refused for their position; a side change (l / r) is the turn. */
    fun moveWall(
        scope: RoomScope,
        access: FurniAccess,
        furniId: Int,
        position: String,
    ) {
        if (!access.mayMove) return FurniOut.error(scope, access.playerId, PlaceError.NotOwner)
        val furni = scope.room.furni[furniId] ?: return
        if (!furni.isWall || !WallPosition.isValid(position)) return
        furni.wallPosition = position
        furni.dirty = true
        scope.out.send(Lane.Now, Audience.Room, ItemUpdateOutCodec, FurniWire.itemUpdate(furni))
    }

    private fun isTopOf(
        scope: RoomScope,
        furni: RoomFurni,
        tiles: List<Point>,
    ): Boolean =
        tiles.all {
            scope.room.furni
                .at(it)
                .topItem() === furni
        }
}
