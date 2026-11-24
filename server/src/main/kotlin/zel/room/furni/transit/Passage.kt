package zel.room.furni.transit

import zel.room.map.Point
import zel.room.unit.RoomUnit

data class Passage(
    val unit: RoomUnit,
    val exit: Point,
)
