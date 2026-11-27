package zel.game.games.snowball

import org.slf4j.LoggerFactory
import zel.game.achievements.GameEvents
import zel.game.earnings.IncomeRewards
import zel.game.games.snowball.SnowballScoreStore.ScoreRow
import java.time.Clock
import java.time.LocalDate
import java.util.SplittableRandom

/** Settles the last finished ISO week once (weekly achievements and prizes) while it has no results row yet. */
class SnowballWeekly(
    private val store: SnowballScoreStore,
    private val effects: Effects,
    private val clock: Clock,
) {
    interface Effects {
        fun progressAchievement(
            userId: Int,
            event: String,
            amount: Int,
        )

        fun reward(
            userId: Int,
            type: Int,
            amount: Int,
            productCode: String,
        ): Boolean

        fun furniExists(classname: String): Boolean
    }

    data class AchievementAward(
        val userId: Int,
        val event: String,
        val amount: Int,
    )

    data class RewardAward(
        val userId: Int,
        val type: Int,
        val amount: Int,
        val productCode: String,
    )

    data class GroupWinner(
        val groupId: Int,
        val players: List<Int>,
    )

    data class Plan(
        val week: LocalDate,
        val achievements: List<AchievementAward>,
        val rewards: List<RewardAward>,
    )

    fun lastFinishedWeek(): LocalDate = SnowballLeaderboards.weekStart(clock, 1)

    fun settleLastWeek(prizes: SnowballWeeklyPrizes): Plan? {
        val week = lastFinishedWeek()
        if (store.weekSettled(week) || !store.settleWeek(week)) {
            return null
        }
        val board = store.userScores(week, 0)
        val groupBoard = store.groupScores(week)
        val groups =
            groupBoard.take(prizes.groupPoints.size).map { GroupWinner(it.id, store.groupScorers(it.id, week)) }
        val plan = plan(week, board, groups, prizes, store.weekSeed(week))
        for (award in plan.achievements) {
            effects.progressAchievement(award.userId, award.event, award.amount)
        }
        for (reward in plan.rewards) {
            if (reward.productCode.isNotEmpty() && !effects.furniExists(reward.productCode)) {
                LOGGER.warn(
                    "Snowball week {}: prize furni {} for user {} has no base item; skipped.",
                    week,
                    reward.productCode,
                    reward.userId,
                )
                continue
            }
            if (!effects.reward(reward.userId, reward.type, reward.amount, reward.productCode)) {
                LOGGER.error("Snowball week {}: prize {} for user {} was not stored.", week, reward, reward.userId)
            }
        }
        LOGGER.info(
            "Snowball week {} settled: {} ranked players, {} achievement steps, {} prizes.",
            week,
            board.size,
            plan.achievements.size,
            plan.rewards.size,
        )
        return plan
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SnowballWeekly::class.java)

        internal const val TOP_TEN = 10

        internal fun plan(
            week: LocalDate,
            board: List<ScoreRow>,
            groups: List<GroupWinner>,
            prizes: SnowballWeeklyPrizes,
            seed: Long,
        ): Plan {
            val topTen = board.take(TOP_TEN).map { AchievementAward(it.id, GameEvents.GAME_SNOWBALL_WEEKLY_TOP_TEN, 1) }
            val best = board.firstOrNull()?.let { AchievementAward(it.id, GameEvents.GAME_SNOWBALL_WEEKLY_BEST, 1) }
            val achievements = topTen + listOfNotNull(best)

            val rewards = mutableListOf<RewardAward>()
            prizes.rankPrizes.take(board.size).forEachIndexed {
                rank,
                prize,
                ->
                rewards.add(RewardAward(board[rank].id, 1, 1, prize))
            }
            if (prizes.luckyPrize.isNotEmpty()) {
                luckyPlayers(board, prizes, seed).forEach { rewards.add(RewardAward(it, 1, 1, prizes.luckyPrize)) }
            }
            prizes.groupPoints.take(groups.size).forEachIndexed { rank, points ->
                if (points > 0) {
                    groups[rank].players.forEach { rewards.add(RewardAward(it, IncomeRewards.POINTS, points, "")) }
                }
            }
            return Plan(week, achievements, rewards.toList())
        }

        internal fun luckyPlayers(
            board: List<ScoreRow>,
            prizes: SnowballWeeklyPrizes,
            seed: Long,
        ): List<Int> {
            val pool = mutableListOf<Int>()
            for (rank in prizes.luckyFrom..minOf(prizes.luckyTo, board.size)) {
                pool.add(board[rank - 1].id)
            }
            val count = minOf(prizes.luckyCount, pool.size)
            val random = SplittableRandom(seed)
            for (i in 0 until count) {
                pool.swap(i, i + random.nextInt(pool.size - i))
            }
            return pool.subList(0, count).toList()
        }

        private fun MutableList<Int>.swap(
            a: Int,
            b: Int,
        ) {
            this[a] = this[b].also { this[b] = this[a] }
        }
    }
}
