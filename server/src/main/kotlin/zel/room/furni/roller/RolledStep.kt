package zel.room.furni.roller

import zel.room.map.Point

data class RolledStep(
    val unitIndex: Int,
    val tile: Point,
    val leftTopId: Int?,
    val newTopId: Int?,
    val dueTick: Long,
)
