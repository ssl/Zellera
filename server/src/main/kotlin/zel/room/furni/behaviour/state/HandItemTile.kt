package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.unit.RoomUnit

/** Serves a unit that has stood on it for a while; one that walks on gets nothing. */
class HandItemTile(
    private val part: StatePart,
    private val dispenser: Dispenser,
) : Steppable {
    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val tile = unit.tile
        part.later(scope, furni, STAND_MILLIS) { inner, target ->
            if (inner.room.units[unit.index] === unit && unit.tile == tile && !unit.isWalking) {
                dispenser.dispense(inner, target, unit)
            }
        }
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    private companion object {
        const val STAND_MILLIS = 3000L
    }
}
