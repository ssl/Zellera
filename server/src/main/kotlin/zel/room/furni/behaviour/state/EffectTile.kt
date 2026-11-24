package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

/** Puts its effect on whoever steps on it and leaves it on after they step off. */
class EffectTile(
    part: StatePart,
) : PressurePlate(part, allTiles = false, wearsEffect = false) {
    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        super.onStepOn(scope, furni, unit)
        if (unit.kind is UnitKind.Pet) return
        val definition = furni.definition
        part.hooks.giveEffect(
            scope,
            unit,
            if (part.hooks.male(unit)) definition.effectMale else definition.effectFemale,
            null,
        )
    }
}
