package zel.wired.variable

import zel.room.RoomScope
import zel.room.unit.RoomUnit

interface PlayerFacts {
    fun accountLevel(
        scope: RoomScope,
        playerId: Int,
    ): Int

    fun achievementScore(
        scope: RoomScope,
        playerId: Int,
    ): Int

    fun gender(
        scope: RoomScope,
        playerId: Int,
    ): Int

    fun favouriteGroup(
        scope: RoomScope,
        playerId: Int,
    ): Int?

    fun hasClub(playerId: Int): Boolean

    fun rightsLevel(
        scope: RoomScope,
        playerId: Int,
    ): Int

    fun muted(
        scope: RoomScope,
        playerId: Int,
    ): Boolean

    fun team(
        scope: RoomScope,
        playerId: Int,
    ): TeamFacts?

    fun transaction(
        scope: RoomScope,
        playerId: Int,
    ): Map<String, Long> = emptyMap()

    fun setUserScore(
        scope: RoomScope,
        unit: RoomUnit,
        score: Int,
    ): Boolean = false
}
