package zel.wired.variable

import zel.room.RoomScope
import zel.room.unit.RoomUnit

interface UnitFacts {
    fun handItem(unit: RoomUnit): Int

    fun holdItem(
        scope: RoomScope,
        unit: RoomUnit,
        item: Int,
    )

    /** Whether the unit holds `@handitem`, which it can with an empty hand once wired gave it. */
    fun handItemHeld(unit: RoomUnit): Boolean

    fun markHandItem(
        scope: RoomScope,
        unit: RoomUnit,
        held: Boolean,
    )

    fun effect(unit: RoomUnit): Int

    fun dance(unit: RoomUnit): Int

    fun asleep(unit: RoomUnit): Boolean

    fun frozen(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean = false
}
