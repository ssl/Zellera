package zel.room.unit.walk

import zel.room.map.Point

data class StepOrigin(
    val tile: Point,
    val z: Double,
    val to: Point,
    val toZ: Double,
)
