package zel.game.achievements

import zel.game.earnings.IncomeRewards
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import java.sql.Connection

internal class LevelRewards(
    val wallet: List<LedgerEntry>,
    val income: List<Int>,
) {
    fun bookIncome(
        connection: Connection,
        playerId: Int,
        at: Int,
    ) {
        for (amount in income) {
            if (amount <= 0) continue
            IncomeRewards.insert(
                connection,
                playerId,
                IncomeRewards.ACHIEVEMENTS,
                IncomeRewards.POINTS,
                amount,
                "",
                at,
            )
        }
    }

    companion object {
        fun of(
            playerId: Int,
            hasClub: Boolean,
            achievement: Achievement,
            earned: List<AchievementLevel>,
        ): LevelRewards {
            val (points, currencies) =
                earned.filter { it.rewardAmount > 0 }.partition {
                    it.rewardCurrency ==
                        EconomyLedger.POINTS
                }
            return LevelRewards(
                wallet = currencies.map { walletReward(playerId, achievement, it) },
                income = points.map { IncomeRewards.clubPoints(it.rewardAmount, hasClub) },
            )
        }

        private fun walletReward(
            playerId: Int,
            achievement: Achievement,
            level: AchievementLevel,
        ) = LedgerEntry(
            key = "achievement/${achievement.id}/${level.level}/$playerId",
            playerId = playerId,
            currency = level.rewardCurrency,
            amount = level.rewardAmount,
            cause = "achievement.level",
            note = achievement.name,
        )
    }
}
