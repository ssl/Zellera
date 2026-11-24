package zel.game.players.inventory

import zel.db.Database

class BadgeCaches(
    database: Database,
) {
    val leaderboard = BadgeLeaderboard(database)
    val ownerCounts = BadgeHolders(database)

    fun invalidate() {
        leaderboard.invalidate()
        ownerCounts.invalidate()
    }
}
