package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.unit.RoomUnit

class Dispenser(
    private val part: StatePart,
    private val effects: Boolean,
) : Usable {
    private val worn = WornEffect(part)

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        if (Near.adjacent(furni, unit.tile)) dispense(scope, furni, unit)
    }

    fun dispense(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (part.pending(scope.room, furni)) return
        if (effects) {
            val own = worn.of(furni, unit)
            part.hooks.giveEffect(scope, unit, if (own > 0) own else furni.definition.randomHandItem(), null)
        } else {
            part.hooks.handItemFromFurni(scope, unit, furni.definition.randomHandItem())
        }
        if (furni.definition.stateCount > 1) {
            StateShow.data(scope, furni, BUSY)
            part.later(scope, furni, RESET_MILLIS, IDLE) { inner, target -> StateShow.data(inner, target, IDLE) }
        }
    }

    private companion object {
        const val IDLE = "0"
        const val BUSY = "1"
        const val RESET_MILLIS = 500L
    }
}
