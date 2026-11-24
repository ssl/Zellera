package zel.game.players

import java.sql.ResultSet

class PlayerTally internal constructor(
    private val store: PlayerStatsStore,
) {
    @Volatile
    var achievementScore = 0

    var respectsReceived = 0
    var respectsGiven = 0

    var respectsLeft = 0
    var petRespectsLeft = 0

    var respectRefillsLeft = 0
    var loginStreak = 0
    var hallOfFamePoints = 0
    var forumPosts = 0

    /** Spent and bought under the stats lock. */
    var marketplaceTokens = 0

    var newAdditionsSeen = 0

    fun refillRespects(perDay: Int): Boolean {
        if (respectRefillsLeft <= 0) return false
        respectRefillsLeft--
        respectsLeft = perDay
        store.saveRespectRefill(respectRefillsLeft, respectsLeft)
        return true
    }

    internal fun read(row: ResultSet) {
        achievementScore = row.getInt("achievement_score")
        respectsReceived = row.getInt("respect_received")
        respectsGiven = row.getInt("respect_given")
        respectsLeft = row.getInt("daily_respect_left")
        petRespectsLeft = row.getInt("daily_pet_respect_left")
        respectRefillsLeft = row.getInt("daily_respect_refills_left")
        loginStreak = row.getInt("login_streak")
        hallOfFamePoints = row.getInt("hall_of_fame_points")
        forumPosts = row.getInt("forum_post_count")
        marketplaceTokens = row.getInt("marketplace_tokens")
        newAdditionsSeen = row.getInt("new_additions_seen")
    }
}
