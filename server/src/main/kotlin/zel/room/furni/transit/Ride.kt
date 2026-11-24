package zel.room.furni.transit

import zel.room.map.Point
import zel.room.unit.RoomUnit

class Ride(
    val unit: RoomUnit,
    val kind: RideKind,
    var furniId: Int,
    var anchor: Point,
    var stage: RideStage,
    var due: Long,
) {
    var destination: Destination? = null

    /** True while [destination] is being read off the room's lock; the ride waits for it. */
    var resolving = false
}
