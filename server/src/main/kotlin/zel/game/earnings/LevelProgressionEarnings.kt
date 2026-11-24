package zel.game.earnings

object LevelProgressionEarnings {
    private val POINTS_BY_LEVEL =
        intArrayOf(
            0,
            40,
            40,
            45,
            45,
            50,
            100,
            110,
            110,
            200,
            120,
            120,
            120,
            130,
            130,
            130,
            140,
            0,
            0,
            0,
            225,
            225,
            225,
            0,
            240,
            0,
            270,
            270,
            270,
            0,
        )

    fun points(level: Int): Int = POINTS_BY_LEVEL.getOrElse(level - 1) { 0 }

    /** A failed level pays on a recount. */
    fun levelsReached(
        rewards: IncomeRewards,
        userId: Int,
        fromLevel: Int,
        toLevel: Int,
    ): Int {
        for (level in fromLevel + 1..toLevel) {
            val amount = points(level)
            if (amount > 0 &&
                !rewards.add(userId, IncomeRewards.LEVEL_PROGRESSION, IncomeRewards.POINTS, amount, "")
            ) {
                return level - 1
            }
        }
        return toLevel
    }
}
