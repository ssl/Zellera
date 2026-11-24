package zel.wired.addon

import zel.room.RoomScope
import zel.room.unit.RoomUnit

fun interface UnitNames {
    fun nameOf(
        scope: RoomScope,
        unit: RoomUnit,
    ): String?
}
