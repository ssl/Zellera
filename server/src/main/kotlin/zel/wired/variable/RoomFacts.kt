package zel.wired.variable

import zel.game.pets.Pet
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.games.GameClock
import zel.room.unit.RoomUnit

interface RoomFacts {
    fun teamScore(
        room: Room,
        color: Int,
    ): Int

    fun teamSize(
        room: Room,
        color: Int,
    ): Int

    fun setTeamScore(
        scope: RoomScope,
        color: Int,
        score: Int,
    ): Boolean = false

    fun pet(
        room: Room,
        unit: RoomUnit,
    ): Pet?

    fun horseRider(
        room: Room,
        unit: RoomUnit,
    ): Int? = null

    fun clock(furni: RoomFurni): GameClock?

    fun wiredTimer(scope: RoomScope): Long

    fun chest(furni: RoomFurni): Map<String, Long> = emptyMap()

    fun teleportTarget(furni: RoomFurni): Int? = null

    /** Its pair is left alone. */
    fun aimTeleport(
        roomId: Int,
        furni: RoomFurni,
        target: RoomFurni,
    ) = Unit

    fun timezone(room: Room): String
}
