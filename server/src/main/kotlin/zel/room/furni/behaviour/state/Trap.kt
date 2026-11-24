package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

/** Holds a player who steps onto it while it is armed. Its definition's params name `effect,milliseconds`. */
class Trap(
    part: StatePart,
) : Multistate(part, toggles = false) {
    private data class Grip(
        val effect: Int,
        val millis: Long,
    )

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (furni.state == DISARMED || unit.kind !is UnitKind.Player) return
        val grip = grip(furni, unit)
        val before = part.hooks.effect(unit)
        part.hooks.stopWalk(unit)
        part.hooks.holdStill(scope, unit)
        if (grip.effect > 0 && before != grip.effect) part.hooks.giveEffect(scope, unit, grip.effect, null)
        val free = { later: RoomScope ->
            part.hooks.letGo(unit)
            // An effect the player put on while held is his to keep.
            if (grip.effect > 0 && part.hooks.effect(unit) == grip.effect && before != grip.effect) {
                part.hooks.giveEffect(later, unit, before, null)
            }
        }
        part.laterOrDropped(scope, furni, grip.millis, free) { later, _ -> free(later) }
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    private fun grip(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Grip {
        val named =
            furni.definition.params
                .split(',')
                .map { it.trim().toLongOrNull() }
        return Grip(
            effect = named.getOrNull(0)?.toInt() ?: effectOf(furni, unit),
            millis = named.getOrNull(1)?.takeIf { it > 0 } ?: HOLD_MILLIS,
        )
    }

    private companion object {
        const val DISARMED = "0"
        const val HOLD_MILLIS = 3000L
    }
}
