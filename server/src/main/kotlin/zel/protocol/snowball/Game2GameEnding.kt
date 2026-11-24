package zel.protocol.snowball

import zel.game.games.snowball.SnowballMode

@JvmRecord
data class Game2GameEnding(
    val timeToNextState: Int,
    val mode: SnowballMode,
    val resultType: Int,
    val winner: Int,
    val teams: List<Team>,
    val mostKillsUserId: Int,
    val mostHitsUserId: Int,
) {
    @JvmRecord
    data class Player(
        val name: String,
        val userId: Int,
        val figure: String,
        val gender: String,
        val score: Int,
        val stats: IntArray,
    ) {
        init {
            require(stats.size == STATS) { "Game2PlayerStatsData has $STATS ints, not ${stats.size}" }
        }

        override fun equals(other: Any?): Boolean =
            other is Player &&
                name == other.name && userId == other.userId && figure == other.figure &&
                gender == other.gender && score == other.score && stats.contentEquals(other.stats)

        override fun hashCode(): Int = listOf(name, userId, figure, gender, score, stats.contentHashCode()).hashCode()

        override fun toString(): String = "Player($name, $userId, $figure, $gender, $score, ${stats.contentToString()})"
    }

    @JvmRecord
    data class Team(
        val teamId: Int,
        val score: Int,
        val players: List<Player>,
    )

    companion object {
        const val RESULT_WIN = 0
        const val RESULT_WIN_OTHER_TEAM_LEFT = 1
        const val RESULT_TIE = 2

        private const val STATS = 10
    }
}
