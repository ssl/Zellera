package zel.room.furni.transit

import zel.room.RoomScope
import zel.room.unit.RoomUnit

/** Every member has a neutral default; called under the room lock. */
interface TransitHooks {
    fun clubMember(unit: RoomUnit): Boolean = false

    fun groupMember(
        unit: RoomUnit,
        groupId: Int,
    ): Boolean = false

    fun effect(unit: RoomUnit): Int = 0

    fun teleported(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun travel(
        scope: RoomScope,
        unit: RoomUnit,
        roomId: Int,
        furniId: Int,
    ): Boolean = false
}
