package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.unit.RoomUnit

class ColorPlate : Steppable {
    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = change(scope, furni, 1)

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = change(scope, furni, -1)

    private fun change(
        scope: RoomScope,
        furni: RoomFurni,
        amount: Int,
    ) {
        val current = furni.state.trim().toIntOrNull() ?: 0
        val top = maxOf(0, furni.definition.stateCount)
        StateShow.data(scope, furni, (current + amount).coerceIn(0, top).toString())
    }
}
