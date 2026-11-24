package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.map.Point

data class FloorMove(
    val furni: RoomFurni,
    val from: Point,
    val fromZ: Double,
    val to: Point,
    val toZ: Double,
)
