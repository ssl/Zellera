package zel.wired.condition

import zel.room.RoomScope
import zel.room.map.Point
import zel.room.unit.RoomUnit

interface UnitFacts {
    fun arrivedAt(
        scope: RoomScope,
        unit: RoomUnit,
    ): Point = unit.tile

    fun effect(unit: RoomUnit): Int

    fun handItem(unit: RoomUnit): Int

    fun asleep(unit: RoomUnit): Boolean

    fun dance(unit: RoomUnit): Int

    fun name(
        scope: RoomScope,
        unit: RoomUnit,
    ): String?

    fun wornBadges(playerId: Int): List<String>

    fun inGroup(
        groupId: Int,
        playerId: Int,
    ): Boolean
}
