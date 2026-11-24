package zel.room.furni

import zel.room.RoomScope
import zel.room.unit.RoomUnit

interface Steppable : FurniBehaviour {
    fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    )

    fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    )
}
