package zel.room.life

import zel.core.rules.rules
import zel.protocol.generated.SleepOut
import zel.protocol.generated.SleepOutCodec
import zel.room.RoomIdle
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class Idle(
    private val hooks: LifeHooks,
    private val times: () -> LifeTimes = { LifeTimes() },
) {
    fun wake(
        scope: RoomScope,
        unit: RoomUnit,
        life: UnitLife,
    ) {
        life.idleTicks = 0
        if (!life.asleep) return
        life.asleep = false
        scope.out.send(Lane.Now, Audience.Room, SleepOutCodec, SleepOut(unit.index, false))
    }

    fun sleep(
        scope: RoomScope,
        unit: RoomUnit,
        life: UnitLife,
    ) {
        if (life.asleep) return
        life.asleep = true
        scope.out.send(Lane.Now, Audience.Room, SleepOutCodec, SleepOut(unit.index, true))
    }

    fun tick(
        scope: RoomScope,
        unit: RoomUnit,
        life: UnitLife,
    ): Boolean {
        life.idleTicks++
        val idle = scope.room.info.idle
        val settings = times()
        if (!settings.autoIdle && !idle.sleepEnabled && !idle.kickEnabled) return false
        if (!life.asleep && life.idleTicks == sleepTicks(idle, settings.sleepTicks) && !hooks.dancing(unit)) {
            life.asleep = true
            scope.out.send(Lane.Now, Audience.Room, SleepOutCodec, SleepOut(unit.index, true))
        }
        val kickTicks = kickTicks(idle) ?: return false
        val owner = (unit.kind as? UnitKind.Player)?.playerId == scope.room.info.ownerId
        return !owner && life.idleTicks >= kickTicks
    }

    companion object {
        fun sleepTicks(
            idle: RoomIdle,
            fallback: Int = rules.sleepSeconds * LifeTiming.TICKS_PER_SECOND,
        ): Int =
            if (idle.sleepEnabled) {
                idle.sleepSeconds.coerceIn(rules.roomSleepSeconds) * LifeTiming.TICKS_PER_SECOND
            } else {
                fallback
            }

        fun kickTicks(idle: RoomIdle): Int? =
            if (rules.idleKick && idle.kickEnabled && idle.kickSeconds > 0) {
                idle.kickSeconds * LifeTiming.TICKS_PER_SECOND
            } else {
                null
            }
    }
}
