package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.Usable
import zel.room.unit.RoomUnit

open class PressurePlate(
    protected val part: StatePart,
    private val allTiles: Boolean,
    private val wearsEffect: Boolean = true,
) : Steppable,
    Usable {
    private val worn = WornEffect(part)

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) = Unit

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        update(scope, furni, leaving = null)
        if (wearsEffect) worn.on(scope, furni, unit)
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        update(scope, furni, leaving = unit)
        if (wearsEffect) worn.off(scope, furni, unit)
    }

    fun update(
        scope: RoomScope,
        furni: RoomFurni,
        leaving: RoomUnit?,
    ) {
        val state = if (pressed(scope, furni, leaving)) ON else OFF
        val changed = state != furni.state.ifEmpty { OFF }
        StateShow.data(scope, furni, state)
        // A pressed or released single plate fires no state changed.
        if (changed && allTiles) scope.raise(RoomEvent.StateChanged(furni.id, null))
    }

    private fun pressed(
        scope: RoomScope,
        furni: RoomFurni,
        leaving: RoomUnit?,
    ): Boolean {
        val standing =
            scope.room.units
                .all()
                .filter { it !== leaving }
                .map { it.tile }
                .toSet()
        val tiles = furni.tiles()
        return if (allTiles) tiles.all { it in standing } else tiles.any { it in standing }
    }

    companion object {
        const val ON = "1"
        const val OFF = "0"
    }
}
