package zel.room.unit.walk

import zel.room.map.Point

object FloodGuard {
    const val PER_SECOND = 15
    private const val WINDOW_MILLIS = 1_000L

    fun accept(
        walk: UnitWalk,
        now: Long,
        target: Point,
    ): Boolean {
        val times = walk.requests
        while (times.isNotEmpty() && now - times.first() >= WINDOW_MILLIS) times.removeFirst()
        if (times.size >= PER_SECOND) {
            walk.refusedGoal = target
            return false
        }
        times.addLast(now)
        return true
    }
}
