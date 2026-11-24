package zel.wired.timing

import zel.room.RoomScope
import zel.room.tick.RoomTiming
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus

/** The status a unit shows for one room tick after wired moved it. */
const val WIRED_MOVE = "wf"

fun RoomPoster.markWiredMove(
    scope: RoomScope,
    unit: RoomUnit,
) {
    unit.setStatus(WIRED_MOVE)
    dropWiredMoveNextTick(scope, unit)
}

fun RoomPoster.dropWiredMoveNextTick(
    scope: RoomScope,
    unit: RoomUnit,
) {
    val room = scope.room
    post(room.info.id, RoomTiming.TICK_MILLIS) { later ->
        if (later.room === room) later.room.units[unit.index]?.dropStatus(WIRED_MOVE)
    }
}

/** What a sit or lay posture adds to the unit's height; 0 when it stands. */
val RoomUnit.postureHeight: Double
    get() =
        (statuses[UnitStatus.SIT] ?: statuses[UnitStatus.LAY])
            ?.substringBefore(' ')
            ?.toDoubleOrNull() ?: 0.0
