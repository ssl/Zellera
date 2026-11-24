package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import kotlin.random.Random

class ClickRandomState(
    part: StatePart,
) : Multistate(part) {
    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? = drawn(furni, part.random)

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean = toggleRandom(scope, furni, part.random)

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean {
        val state = drawn(furni, random) ?: return false
        StateShow.set(furni, state)
        return true
    }

    private fun drawn(
        furni: RoomFurni,
        random: Random,
    ): String? {
        val states = furni.definition.stateCount
        return if (states <= 0) null else random.nextInt(states).toString()
    }
}
