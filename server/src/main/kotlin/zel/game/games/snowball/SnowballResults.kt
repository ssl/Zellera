package zel.game.games.snowball

import zel.game.achievements.GameEvents
import zel.protocol.snowball.Game2GameEnding

data class SnowballResults(
    val mode: SnowballMode,
    val resultType: Int,
    val winner: Int,
    val teams: List<Game2GameEnding.Team>,
    val mostKillsUserId: Int,
    val mostHitsUserId: Int,
) {
    fun won(player: SnowballMatch.Player): Boolean {
        if (resultType != Game2GameEnding.RESULT_WIN && resultType != Game2GameEnding.RESULT_WIN_OTHER_TEAM_LEFT) {
            return false
        }
        return if (mode.isDeathmatch) player.userId == winner else player.team == winner
    }

    fun ending(rematchSeconds: Int): Game2GameEnding =
        Game2GameEnding(rematchSeconds, mode, resultType, winner, teams, mostKillsUserId, mostHitsUserId)

    fun achievements(
        match: SnowballMatch,
        finishers: List<Int>,
    ): List<SnowballWeekly.AchievementAward> {
        val awards = mutableListOf<SnowballWeekly.AchievementAward>()
        for (player in match.players) {
            val userId = player.userId
            val stats = match.stats(userId)
            if (userId !in finishers || stats == null) {
                continue
            }
            add(awards, userId, GameEvents.GAME_SNOWBALL_TOTAL_SCORE, match.score(userId))
            add(awards, userId, GameEvents.GAME_SNOWBALL_WIN, if (won(player)) 1 else 0)
            add(awards, userId, GameEvents.GAME_SNOWBALL_HIT, stats.snowballHits)
            add(awards, userId, GameEvents.GAME_SNOWBALL_TREE_HIT, stats.treesHit)
        }
        return awards
    }

    companion object {
        fun of(
            match: SnowballMatch,
            forfeitWinner: Int,
        ): SnowballResults {
            val mostKills = mostOf(match) { it.kills }
            val mostHits = mostOf(match) { it.snowballHits }
            return if (match.mode.isDeathmatch) {
                deathmatch(match, forfeitWinner, mostKills, mostHits)
            } else {
                team(match, forfeitWinner, mostKills, mostHits)
            }
        }

        private fun team(
            match: SnowballMatch,
            forfeitWinner: Int,
            mostKills: Int,
            mostHits: Int,
        ): SnowballResults {
            val blue = match.teamScore(1)
            val red = match.teamScore(2)
            val resultType: Int
            val winner: Int
            when {
                forfeitWinner > 0 -> {
                    resultType = Game2GameEnding.RESULT_WIN_OTHER_TEAM_LEFT
                    winner = forfeitWinner
                }

                blue == red -> {
                    resultType = Game2GameEnding.RESULT_TIE
                    winner = 0
                }

                else -> {
                    resultType = Game2GameEnding.RESULT_WIN
                    winner = if (blue > red) 1 else 2
                }
            }
            val teams =
                (1..SnowballMode.TEAM.numberOfTeams).map { teamId ->
                    val rows = match.players.filter { it.team == teamId }.map { row(match, it) }
                    Game2GameEnding.Team(teamId, match.teamScore(teamId), rows)
                }
            return SnowballResults(SnowballMode.TEAM, resultType, winner, teams, mostKills, mostHits)
        }

        private fun deathmatch(
            match: SnowballMatch,
            forfeitWinner: Int,
            mostKills: Int,
            mostHits: Int,
        ): SnowballResults {
            val ranked =
                match.players.sortedWith(
                    compareBy<SnowballMatch.Player> { -match.score(it.userId) }.thenBy { it.userId },
                )
            val resultType: Int
            val winner: Int
            if (forfeitWinner > 0) {
                resultType = Game2GameEnding.RESULT_WIN_OTHER_TEAM_LEFT
                winner = forfeitWinner
            } else {
                val best = ranked.firstOrNull()?.let { match.score(it.userId) } ?: 0
                val shared = ranked.size > 1 && match.score(ranked[1].userId) == best
                resultType = if (shared || ranked.isEmpty()) Game2GameEnding.RESULT_TIE else Game2GameEnding.RESULT_WIN
                winner = if (resultType == Game2GameEnding.RESULT_WIN) ranked[0].userId else 0
            }
            val rows = ranked.map { row(match, it) }
            val total = ranked.sumOf { match.score(it.userId) }
            val teams = listOf(Game2GameEnding.Team(SnowballGame.DEATHMATCH_TEAM, total, rows))
            return SnowballResults(SnowballMode.DEATHMATCH, resultType, winner, teams, mostKills, mostHits)
        }

        private fun row(
            match: SnowballMatch,
            player: SnowballMatch.Player,
        ): Game2GameEnding.Player {
            val score = match.score(player.userId)
            return Game2GameEnding.Player(
                player.name,
                player.userId,
                player.figure,
                player.gender,
                score,
                checkNotNull(match.stats(player.userId)).toWire(score),
            )
        }

        /** A tie goes to the lowest user id; with nobody scoring, the lowest user id of the match. */
        internal fun mostOf(
            match: SnowballMatch,
            stat: (SnowballPlayerStats) -> Int,
        ): Int {
            var best = 0
            var bestValue = 0
            var lowest = 0
            for (player in match.players) {
                if (lowest == 0 || player.userId < lowest) {
                    lowest = player.userId
                }
                val value = stat(checkNotNull(match.stats(player.userId)))
                if (value > bestValue || (value == bestValue && value > 0 && player.userId < best)) {
                    best = player.userId
                    bestValue = value
                }
            }
            return if (bestValue > 0) best else lowest
        }

        private fun add(
            awards: MutableList<SnowballWeekly.AchievementAward>,
            userId: Int,
            event: String,
            amount: Int,
        ) {
            if (amount > 0) {
                awards.add(SnowballWeekly.AchievementAward(userId, event, amount))
            }
        }
    }
}
