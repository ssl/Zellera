package zel.room.unit.walk

import zel.room.unit.RoomUnit
import zel.room.unit.RoomUnits

/** Owned by the room's lock. */
class RoomWalks {
    private val byIndex = HashMap<Int, UnitWalk>()

    fun of(unit: RoomUnit): UnitWalk = byIndex.getOrPut(unit.index) { UnitWalk() }

    fun prune(units: RoomUnits) {
        byIndex.keys.retainAll { units[it] != null }
    }
}
