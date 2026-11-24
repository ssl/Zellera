package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

internal class WornEffect(
    private val part: StatePart,
) {
    fun of(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Int =
        when {
            unit.kind is UnitKind.Pet -> 0
            part.hooks.male(unit) -> furni.definition.effectMale
            else -> furni.definition.effectFemale
        }

    fun on(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val effect = of(furni, unit)
        if (effect > 0 && part.hooks.effect(unit) != effect) part.hooks.giveEffect(scope, unit, effect, null)
    }

    /** Takes the effect off, unless the avatar's next tile is topped by a furni with the same effect. */
    fun off(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val effect = of(furni, unit)
        if (effect <= 0) return
        val next = nextTop(scope, furni, unit)
        if (next != null && of(next, unit) == effect) return
        part.hooks.giveEffect(scope, unit, next?.let { of(it, unit) } ?: 0, null)
    }

    private fun nextTop(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ): RoomFurni? {
        val next = unit.path.firstOrNull() ?: return null
        return scope.room.furni
            .at(next)
            .topItem()
            ?.takeIf { it !== furni }
    }
}
