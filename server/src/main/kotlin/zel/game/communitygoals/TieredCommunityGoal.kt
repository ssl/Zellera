package zel.game.communitygoals

import zel.db.orFallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.IntSupplier

class TieredCommunityGoal(
    private val table: TieredGoalTable,
    private val unixTime: IntSupplier,
) {
    data class Progress(
        val isOver: Boolean,
        val personalScore: Int,
        val personalRank: Int,
        val totalScore: Int,
        val reach: ActiveGoal.Reach,
        val code: String,
        val secondsLeft: Int,
        val rewardLimits: List<Int>,
    ) {
        companion object {
            val NONE = Progress(true, 0, 0, 0, ActiveGoal.Reach(0, 0, 0), "", 0, emptyList())
        }
    }

    /** Where [playerId] stands; no goal when the database cannot say. */
    fun progressFor(playerId: Int): Progress =
        LOGGER.orFallback("Reading the community goal", Progress.NONE) { standingOf(playerId) }

    private fun standingOf(playerId: Int): Progress {
        val now = unixTime.asInt
        val goal = table.shownGoal(now) ?: return Progress.NONE
        val standing = table.standing(goal.id, playerId)
        return Progress(
            goal.isOver(now),
            standing.score,
            standing.rank,
            standing.total,
            goal.reach(standing.total),
            goal.code,
            goal.secondsLeft(now),
            goal.rewardLimits,
        )
    }

    /** A goal that closed takes no more votes. */
    fun vote(
        playerId: Int,
        optionId: Int,
    ): Boolean {
        if (playerId <= 0) return false
        return LOGGER.orFallback("Casting a community goal vote of player $playerId", false) {
            val now = unixTime.asInt
            val goal = table.shownGoal(now)?.takeUnless { it.isOver(now) }
            goal != null && table.castVote(goal.id, playerId, optionId)
        }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(TieredCommunityGoal::class.java)
    }
}
