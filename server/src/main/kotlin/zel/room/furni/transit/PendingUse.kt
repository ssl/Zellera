package zel.room.furni.transit

import zel.room.map.Point

data class PendingUse(
    val furniId: Int,
    val front: Point,
)
