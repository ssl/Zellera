package zel.room.chat

import zel.room.RoomScope
import zel.room.unit.RoomUnit
import zel.room.unit.walk.WalkHooks

class DanceWalkHooks(
    private val dances: Dances,
    private val next: WalkHooks = WalkHooks.None,
) : WalkHooks by next {
    override fun postureTaken(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        dances.stop(scope, unit)
        next.postureTaken(scope, unit)
    }
}
