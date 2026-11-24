package zel.game.players.level

import zel.game.players.inventory.BadgeLeaderboard

object AccountScoring {
    private const val ACHIEVEMENT_BADGE_PREFIX = "ACH_"

    fun levelAfter(
        reached: Int,
        points: Int,
    ): Int = maxOf(reached, AccountLevelTable.levelOf(points))

    fun badgeInputs(
        codes: Collection<String>,
        furni: Int,
    ): LevelInputs =
        LevelInputs(
            achievements = BadgeLeaderboard.achievementLevel(HashSet(codes)),
            badges = codes.toSet().count { !it.startsWith(ACHIEVEMENT_BADGE_PREFIX) },
            furni = furni,
        )

    fun hostingPoints(
        levelMinutes: Long,
        divisor: Int,
    ): Int = (levelMinutes / divisor.coerceAtLeast(1)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun points(
        inputs: LevelInputs,
        hostingLevelMinutes: Long,
        hostingDivisor: Int,
    ): Int =
        CountedScores
            .of(
                inputs.achievements,
                inputs.badges,
                hostingPoints(hostingLevelMinutes, hostingDivisor),
                inputs.furni,
            ).total
}
