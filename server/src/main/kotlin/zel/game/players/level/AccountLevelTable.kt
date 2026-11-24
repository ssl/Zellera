package zel.game.players.level

import zel.game.players.Player
import zel.game.players.inventory.BadgeLeaderboard

object AccountLevelTable {
    internal val starts: List<Int> =
        listOf(
            0,
            30,
            35,
            45,
            60,
            75,
            95,
            115,
            140,
            165,
            190,
            215,
            240,
            270,
            300,
            340,
            390,
            450,
            520,
            600,
            700,
            800,
            900,
            1100,
            1300,
            1600,
            2000,
            2500,
            3200,
            4000,
        )

    val top: Int get() = starts.size

    data class Rung(
        val level: Int,
        val nextStart: Int,
    )

    fun reachedWith(points: Int): Rung = kept(maxOf(1, starts.count { it <= points }), points)

    fun kept(
        level: Int,
        points: Int,
    ): Rung = Rung(level, starts.getOrElse(level) { maxOf(0, points) })

    fun levelOf(points: Int): Int = reachedWith(points).level

    fun levelOf(player: Player?): Int = maxOf(1, player?.accountLevel ?: 1)

    fun levelOf(
        playerId: Int,
        online: Player?,
        leaderboard: BadgeLeaderboard?,
    ): Int =
        when {
            playerId <= 0 -> 1
            online != null -> levelOf(online)
            else -> leaderboard?.accountStanding(playerId)?.level ?: 1
        }
}
