package zel.game.players.level

data class AccountStanding(
    val playerId: Int,
    val inputs: LevelInputs,
    val hostingLevelMinutes: Long,
    val countedPoints: Int,
    val level: Int,
    val rewardedLevel: Int,
) {
    fun recounted(
        held: LevelInputs,
        hostingDivisor: Int,
    ): AccountStanding {
        val inputs = held.copy(achievements = maxOf(held.achievements, this.inputs.achievements))
        val points = AccountScoring.points(inputs, hostingLevelMinutes, hostingDivisor)
        return copy(inputs = inputs, countedPoints = points, level = AccountScoring.levelAfter(level, points))
    }

    companion object {
        fun first(
            playerId: Int,
            inputs: LevelInputs,
        ): AccountStanding {
            val paid = AccountLevelTable.levelOf(inputs.achievements)
            return AccountStanding(playerId, inputs, 0, inputs.achievements, paid, paid)
        }
    }
}
