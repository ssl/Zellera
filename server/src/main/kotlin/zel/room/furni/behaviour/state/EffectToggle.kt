package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit

class EffectToggle(
    part: StatePart,
) : Multistate(part) {
    override fun mayUse(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        playerId: Int,
    ): Boolean {
        if (!part.hooks.hasRights(scope, playerId)) return false
        val state = furni.state.trim().toIntOrNull() ?: 0
        return state < furni.definition.stateCount - 1 && part.hooks.effect(unit) == effectOf(furni, unit)
    }
}
