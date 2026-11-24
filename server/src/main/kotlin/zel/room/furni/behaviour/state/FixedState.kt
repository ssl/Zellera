package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import kotlin.random.Random

class FixedState(
    part: StatePart,
) : Multistate(part) {
    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? = null

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean = false

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = false
}
