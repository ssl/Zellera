package zel.room.furni.transit

import zel.room.unit.RoomUnit
import zel.room.unit.walk.WalkHooks
import java.util.Collections
import java.util.IdentityHashMap

/** A unit is held only under its room's lock; the set is shared by all rooms. */
class TransitHolds(
    private val next: WalkHooks = WalkHooks.None,
) : WalkHooks by next {
    private val held: MutableSet<RoomUnit> = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))

    override fun mayWalk(unit: RoomUnit): Boolean = unit !in held && next.mayWalk(unit)

    fun holds(unit: RoomUnit): Boolean = unit in held

    fun hold(unit: RoomUnit) {
        held += unit
    }

    fun release(unit: RoomUnit) {
        held -= unit
    }
}
