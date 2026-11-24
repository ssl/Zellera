package zel.room.furni.roller

import zel.room.unit.RoomUnits

/** Owned by the room's lock. */
class RollerRoom {
    var ticks: Long = 0

    var sinceRoll: Int = Int.MAX_VALUE

    var speed: Int? = null

    val owed = ArrayList<RolledStep>()

    fun prune(units: RoomUnits) {
        owed.removeAll { units[it.unitIndex] == null }
    }
}
