package zel.room.furni.place

import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.map.Point

interface Placeable : FurniBehaviour {
    fun beforeAdded(furni: RoomFurni) {}

    fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {}

    fun released(
        scope: RoomScope,
        furni: RoomFurni,
    ): List<Point> = emptyList()

    /** Picked up: right after its remove, before the heights of the tiles it frees. */
    fun onLifted(
        scope: RoomScope,
        furni: RoomFurni,
    ) {}

    /** Picked up: right after the heights of the tiles it frees, a sweep's too. */
    fun onTilesFreed(
        scope: RoomScope,
        furni: RoomFurni,
    ) {}

    fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {}

    fun onMoved(
        scope: RoomScope,
        furni: RoomFurni,
        oldTiles: List<Point>,
    ) {}
}
