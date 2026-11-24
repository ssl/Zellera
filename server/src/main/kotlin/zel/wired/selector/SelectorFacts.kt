package zel.wired.selector

import zel.room.RoomScope
import zel.room.unit.RoomUnit

/** Called under the room's lock. */
interface SelectorFacts {
    fun playerName(
        scope: RoomScope,
        unit: RoomUnit,
    ): String?

    /** An avatar's, bot's or pet's name. */
    fun unitName(
        scope: RoomScope,
        unit: RoomUnit,
    ): String? = playerName(scope, unit)

    fun handItem(unit: RoomUnit): Int

    fun dance(unit: RoomUnit): Int

    fun asleep(unit: RoomUnit): Boolean

    fun team(
        scope: RoomScope,
        unit: RoomUnit,
    ): Int

    fun inGroup(
        scope: RoomScope,
        unit: RoomUnit,
        groupId: Int,
    ): Boolean

    fun recentActions(
        scope: RoomScope,
        unit: RoomUnit,
    ): Map<Int, ActionNote>
}
