package zel.room.furni

import zel.protocol.generated.HeightMapUpdateOut
import zel.room.Room
import zel.room.map.MapMessages
import zel.room.map.Point
import zel.room.map.TileValue

class HeightMapChange private constructor(
    private val room: Room,
    private val before: Map<Point, Short>,
) {
    fun with(hidden: List<Point>): HeightMapChange =
        if (hidden.isEmpty()) this else HeightMapChange(room, before + hidden.associateWith { MapMessages.VOID_VALUE })

    /** This change and [later] as one: a tile in both keeps its height from before this one. */
    operator fun plus(later: HeightMapChange): HeightMapChange = HeightMapChange(room, later.before + before)

    fun updates(): List<HeightMapUpdateOut> =
        MapMessages.heightMapUpdates(
            before.keys.toList(),
            before = { requireNotNull(before[it]) },
            after = { RoomHeights.value(room, it) },
        )

    fun allTiles(): List<HeightMapUpdateOut> =
        MapMessages.heightMapUpdates(before.keys.map { TileValue(it, RoomHeights.value(room, it)) })

    companion object {
        fun of(
            room: Room,
            tiles: List<Point>,
        ): HeightMapChange = HeightMapChange(room, tiles.distinct().associateWith { RoomHeights.value(room, it) })
    }
}
