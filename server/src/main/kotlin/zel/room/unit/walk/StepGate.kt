package zel.room.unit.walk

import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit

interface StepGate : FurniBehaviour {
    fun lets(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean

    fun refused(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    )
}
