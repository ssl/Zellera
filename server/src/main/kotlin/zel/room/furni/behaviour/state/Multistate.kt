package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.unit.RoomUnit
import kotlin.random.Random

open class Multistate(
    protected val part: StatePart,
    private val toggles: Boolean = true,
) : Usable,
    Steppable,
    Toggleable {
    private val worn = WornEffect(part)

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        if (!mayUse(scope, furni, unit, playerId)) return
        val next = clicked(scope, furni) ?: return
        show(scope, furni, next)
        scope.raise(RoomEvent.StateChanged(furni.id, unit.index))
    }

    open fun mayUse(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        playerId: Int,
    ): Boolean = part.hooks.hasRights(scope, playerId)

    open fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? = stepped(furni, backward = false)

    open fun show(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) = StateShow.data(scope, furni, state)

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        if (!toggles) return false
        val next = stepped(furni, backward) ?: return false
        StateShow.set(furni, next)
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean {
        if (!toggles) return false
        val states = furni.definition.stateCount
        StateShow.set(furni, (if (states > 0) random.nextInt(states) else 0).toString())
        return true
    }

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = worn.on(scope, furni, unit)

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = worn.off(scope, furni, unit)

    protected fun effectOf(
        furni: RoomFurni,
        unit: RoomUnit,
    ): Int = worn.of(furni, unit)

    companion object {
        fun stepped(
            furni: RoomFurni,
            backward: Boolean,
        ): String? = stepped(furni.state, furni.definition.stateCount, backward)

        fun stepped(
            state: String,
            states: Int,
            backward: Boolean,
        ): String? {
            if (states <= 1) return null
            val current = state.trim().toIntOrNull() ?: 0
            return Math.floorMod(current + if (backward) -1 else 1, states).toString()
        }
    }
}
