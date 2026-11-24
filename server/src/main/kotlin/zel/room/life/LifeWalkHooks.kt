package zel.room.life

import zel.room.RoomScope
import zel.room.unit.RoomUnit
import zel.room.unit.walk.WalkHooks

class LifeWalkHooks(
    private val life: RoomLife,
    private val next: WalkHooks = WalkHooks.None,
) : WalkHooks by next {
    override fun effect(unit: RoomUnit): Int = life.effect(unit)

    override fun asleep(unit: RoomUnit): Boolean = life.asleep(unit)

    override fun active(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        life.active(scope, unit)
        next.active(scope, unit)
    }
}
