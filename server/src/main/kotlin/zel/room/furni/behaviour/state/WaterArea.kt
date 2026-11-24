package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.WaterPools
import zel.room.furni.place.Placeable
import zel.room.map.Point

class WaterArea(
    part: StatePart,
) : Multistate(part, toggles = false),
    Placeable {
    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? = null

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) = WaterPools.refreshAround(scope, furni.tiles())

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) = WaterPools.refreshAround(scope, furni.tiles())

    override fun onMoved(
        scope: RoomScope,
        furni: RoomFurni,
        oldTiles: List<Point>,
    ) = WaterPools.refreshAround(scope, oldTiles + furni.tiles())
}
