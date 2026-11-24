package zel.room.furni

import zel.protocol.generated.HeightMapOut
import zel.room.Room
import zel.room.map.MapMessages
import zel.room.map.Point

/** Call under the room lock. */
object RoomHeights {
    fun stackingHeight(
        room: Room,
        tiles: List<Point>,
        exclude: RoomFurni? = null,
    ): Double? {
        var landing: Double? = null
        for (tile in tiles) {
            val floor = room.floor.floorZ(tile) ?: return null
            val height = room.furni.at(tile).stackingHeight(floor, exclude) ?: return null
            landing = maxOf(landing ?: height, height)
        }
        return landing
    }

    fun value(
        room: Room,
        tile: Point,
    ): Short {
        val floor = room.floor.floorZ(tile) ?: return MapMessages.VOID_VALUE
        return room.furni.at(tile).heightMapValue(floor)
    }

    /** The height the HeightMap gives [tile], unrounded; null off the floor. */
    fun mapZ(
        room: Room,
        tile: Point,
    ): Double? {
        val floor = room.floor.floorZ(tile) ?: return null
        return room.furni.at(tile).heightMapZ(floor)
    }

    fun heightMap(room: Room): HeightMapOut = MapMessages.heightMap(room.floor) { value(room, it) }
}
