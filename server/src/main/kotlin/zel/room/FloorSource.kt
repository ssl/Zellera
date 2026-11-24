package zel.room

import zel.room.map.FloorPlan

fun interface FloorSource {
    fun floorOf(info: RoomInfo): FloorPlan?
}
