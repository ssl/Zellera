package zel.room.games

import zel.room.RoomScope
import zel.room.unit.RoomUnit
import zel.util.MILLIS_PER_SECOND
import kotlin.math.max
import kotlin.math.roundToLong

open class TeamMember(
    val unit: RoomUnit,
    val playerId: Int,
    val color: TeamColor,
) {
    var score: Int = 0
        protected set

    private var joinedAtMillis = 0L

    open fun reset() {
        score = 0
    }

    fun startWith(points: Int) {
        score = points
    }

    /** Adds [amount] (never below 0) and returns what really changed. */
    fun add(amount: Int): Int {
        val before = score
        score = max(0, score + amount)
        return score - before
    }

    fun joinedAt(runningMillis: Long) {
        joinedAtMillis = runningMillis
    }

    fun secondsPlayed(runningMillis: Long): Int =
        max(0L, ((runningMillis - joinedAtMillis) / MILLIS_PER_SECOND.toDouble()).roundToLong()).toInt()

    fun present(scope: RoomScope): Boolean = scope.room.units[unit.index] === unit
}
