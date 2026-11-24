package zel.room.furni.place

import zel.room.RoomScope
import zel.room.map.Point

fun interface UnitsOnTiles {
    fun furniChanged(
        scope: RoomScope,
        oldTiles: List<Point>,
        newTiles: List<Point>,
    )

    /** A wired move landed a furni on [newTiles] (from [oldTiles]); by default the same as [furniChanged]. */
    fun furniLanded(
        scope: RoomScope,
        oldTiles: List<Point>,
        newTiles: List<Point>,
    ) = furniChanged(scope, oldTiles, newTiles)

    companion object {
        val None: UnitsOnTiles = UnitsOnTiles { _, _, _ -> }
    }
}
