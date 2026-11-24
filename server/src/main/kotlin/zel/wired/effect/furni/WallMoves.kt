package zel.wired.effect.furni

import zel.protocol.generated.ItemUpdateOutCodec
import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.firing.Firing
import zel.wired.firing.Movement

class WallMoves(
    private val moves: FurniMoves,
) {
    fun moveTo(
        firing: Firing,
        item: RoomFurni,
        tile: Point,
    ) {
        if (!firing.scope.room.floor
                .contains(tile)
        ) {
            return
        }
        val place = WallPlace.of(item) ?: return
        val altitude = place.altitude(firing.scope.room)
        val next = place.copy(tile = tile, altitude = altitude).onWallAt(firing.scope.room, altitude)
        slide(firing, item, place, next)
    }

    /** Puts [item] on [spot]'s tile, local x and side at [spot]'s altitude; the local y follows from the altitude. */
    fun placeAt(
        firing: Firing,
        item: RoomFurni,
        spot: WallPlace,
    ) {
        val room = firing.scope.room
        val altitude = spot.altitude ?: return
        if (!room.floor.contains(spot.tile)) return
        val place = WallPlace.of(item) ?: return
        slide(firing, item, place, spot.onWallAt(room, altitude))
    }

    fun lift(
        firing: Firing,
        item: RoomFurni,
        altitude: Int,
    ) {
        val place = WallPlace.of(item) ?: return
        slide(firing, item, place, place.onWallAt(firing.scope.room, altitude.coerceIn(0, MAX_ALTITUDE)))
    }

    /** Onto [spot] on the same wall tile and local x, only the side changing: an ItemUpdate, no slide. */
    fun changeSide(
        firing: Firing,
        item: RoomFurni,
        spot: WallPlace,
    ) {
        val altitude = spot.altitude ?: return
        item.wallPosition = spot.onWallAt(firing.scope.room, altitude).text()
        item.dirty = true
        firing.moved += item.id
        firing.scope.out.send(Lane.AfterUnitUpdate, Audience.Room, ItemUpdateOutCodec, FurniWire.itemUpdate(item))
    }

    fun turn(
        firing: Firing,
        item: RoomFurni,
    ) {
        val room = firing.scope.room
        val place = WallPlace.of(item) ?: return
        val altitude = place.altitude(room)
        val width = if (room.floor.smallScale) WallPlace.SMALL_WIDTH else WallPlace.WIDTH
        val turned = place.copy(right = !place.right, localX = width - place.localX, altitude = altitude)
        item.wallPosition = turned.text()
        item.dirty = true
        firing.moved += item.id
        firing.scope.out.send(
            Lane.AfterUnitUpdate,
            Audience.Room,
            ItemUpdateOutCodec,
            FurniWire.itemUpdate(item),
        )
    }

    private fun slide(
        firing: Firing,
        item: RoomFurni,
        from: WallPlace,
        to: WallPlace,
    ) {
        item.wallPosition = to.text()
        item.dirty = true
        firing.moved += item.id
        val style = moves.style(firing)
        if (style.animated) {
            firing.flash.movements.add(Movement.WallMove(item.id, to.right, from.spot(), to.spot(), style.duration))
        } else {
            firing.scope.out.send(
                Lane.AfterUnitUpdate,
                Audience.Room,
                ItemUpdateOutCodec,
                FurniWire.itemUpdate(item),
            )
        }
    }

    private fun WallPlace.onWallAt(
        room: Room,
        altitude: Int,
    ): WallPlace = copy(localY = localYAt(room, altitude), altitude = altitude)

    private fun WallPlace.spot() = Movement.WallSpot(tile.x, tile.y, localX, localY)

    private companion object {
        const val MAX_ALTITUDE = 8000
    }
}
