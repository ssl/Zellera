package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.tick.RoomTiming
import zel.room.unit.RoomUnit

/** Looks once a tick, up to [ticks] times while the unit still walks, whether it reached its tile beside a furni. */
class ArrivalWatch(
    private val later: (RoomScope, RoomFurni, Long, (RoomScope, RoomFurni) -> Unit) -> Unit,
    private val ticks: Int,
    /** True: the unit must also have stopped on the tile; walking over it does not count. */
    private val mustStop: Boolean,
    private val arrived: (RoomScope, RoomFurni, RoomUnit) -> Unit,
) {
    fun watch(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        goal: Point,
    ) = watch(scope, furni, unit, goal, ticks)

    private fun watch(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        goal: Point,
        left: Int,
    ) {
        later(scope, furni, RoomTiming.TICK_MILLIS) { inner, target ->
            when {
                inner.room.units[unit.index] !== unit -> Unit
                unit.tile == goal && !(mustStop && unit.isWalking) -> arrived(inner, target, unit)
                left > 0 && unit.isWalking -> watch(inner, target, unit, goal, left - 1)
            }
        }
    }
}
