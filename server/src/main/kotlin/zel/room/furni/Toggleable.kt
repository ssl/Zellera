package zel.room.furni

import zel.room.RoomScope
import kotlin.random.Random

interface Toggleable : FurniBehaviour {
    fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean

    fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean
}
