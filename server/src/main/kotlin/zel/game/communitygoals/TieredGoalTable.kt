package zel.game.communitygoals

import zel.db.Database

private const val SHOWN_GOAL_SQL =
    "SELECT id, code, closes_at FROM community_goals WHERE opens_at <= ? ORDER BY opens_at DESC, id DESC LIMIT 1"
private const val TIERS_SQL =
    "SELECT threshold, rewarded_players FROM community_goal_levels WHERE goal_id = ? ORDER BY level"
private const val STANDING_SQL =
    "SELECT (SELECT COALESCE(SUM(score), 0) FROM community_goal_scores WHERE goal_id = g.id) AS total, " +
        "mine.score, " +
        "(SELECT COUNT(*) FROM community_goal_scores a WHERE a.goal_id = g.id AND a.score > mine.score) AS ahead " +
        "FROM community_goals g LEFT JOIN community_goal_scores mine ON mine.goal_id = g.id AND mine.player_id = ? " +
        "WHERE g.id = ?"
private const val VOTE_SQL =
    "INSERT IGNORE INTO community_goal_votes (goal_id, player_id, option_id) " +
        "SELECT goal_id, ?, option_id FROM community_goal_options WHERE goal_id = ? AND option_id = ?"

class TieredGoalTable(
    database: Database,
) {
    private val sql = database.queries

    /** [total] is every player's score together; [rank] is 1 + the players with a higher score, 0 without a score. */
    data class Standing(
        val total: Int,
        val score: Int,
        val rank: Int,
    ) {
        companion object {
            val NONE = Standing(0, 0, 0)
        }
    }

    fun shownGoal(now: Int): ActiveGoal? {
        val head =
            sql.select(SHOWN_GOAL_SQL, now).first {
                Triple(it.getInt("id"), it.getString("code"), it.getLong("closes_at").toInt())
            } ?: return null
        val tiers =
            sql.select(TIERS_SQL, head.first).rows { row ->
                ActiveGoal.Level(row.getInt("threshold"), row.getInt("rewarded_players").takeUnless { row.wasNull() })
            }
        return ActiveGoal(head.first, head.second, tiers, head.third)
    }

    fun standing(
        goalId: Int,
        playerId: Int,
    ): Standing =
        sql.select(STANDING_SQL, playerId, goalId).first { row ->
            val total = row.getLong("total").coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val score = row.getInt("score")
            if (row.wasNull()) Standing(total, 0, 0) else Standing(total, score, row.getInt("ahead") + 1)
        } ?: Standing.NONE

    /** False for an option the goal does not have and for a player who voted before. */
    fun castVote(
        goalId: Int,
        playerId: Int,
        choice: Int,
    ): Boolean = sql.write(VOTE_SQL, playerId, goalId, choice) > 0
}
