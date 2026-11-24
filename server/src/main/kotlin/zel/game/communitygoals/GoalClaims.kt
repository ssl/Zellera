package zel.game.communitygoals

import zel.db.Database

class GoalClaims(
    database: Database,
) {
    private val sql = database.queries

    fun rewarded(
        goalCode: String,
        playerId: Int,
    ): Boolean =
        sql
            .select("SELECT 1 FROM community_goal_claims WHERE goal_code = ? AND player_id = ?", goalCode, playerId)
            .first { true } ?: false

    fun markRewarded(
        goalCode: String,
        playerId: Int,
        at: Int,
    ): Boolean =
        sql.write(
            "INSERT IGNORE INTO community_goal_claims (goal_code, player_id, claimed_at) VALUES (?, ?, ?)",
            goalCode,
            playerId,
            at,
        ) > 0
}
