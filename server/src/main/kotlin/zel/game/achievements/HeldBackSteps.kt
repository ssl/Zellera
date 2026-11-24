package zel.game.achievements

import zel.game.players.Player

/** Holds a player's steps on an achievement and hands their sum to [due] after a pause the first step starts. */
internal class HeldBackSteps(
    private val schedule: (Runnable, Long) -> Unit,
    private val due: (Player, Achievement, Int) -> Unit,
) {
    private val waiting = HashMap<Player, HashMap<Achievement, Int>>()

    fun add(
        player: Player,
        achievement: Achievement,
        amount: Int,
    ) {
        val startsTimer =
            synchronized(waiting) {
                val mine = waiting.getOrPut(player, ::HashMap)
                val before = mine[achievement]
                mine[achievement] = saturatingSum(before ?: 0, amount)
                before == null
            }
        if (startsTimer) schedule(Runnable { release(player, achievement) }, HOLD_BACK_MS)
    }

    fun drain(player: Player): Map<Achievement, Int> = synchronized(waiting) { waiting.remove(player).orEmpty() }

    private fun release(
        player: Player,
        achievement: Achievement,
    ) {
        val amount =
            synchronized(waiting) {
                val mine = waiting[player] ?: return
                val amount = mine.remove(achievement) ?: return
                if (mine.isEmpty()) waiting.remove(player)
                amount
            }
        due(player, achievement, amount)
    }

    private fun saturatingSum(
        a: Int,
        b: Int,
    ): Int = (a.toLong() + b).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    companion object {
        /** Its steps wait a moment, so a burst of placements is one step. */
        const val HELD_BACK_EVENT = GameEvents.ROOM_BUILD_PASS_FURNI
        const val HOLD_BACK_MS = 10_000L
    }
}
