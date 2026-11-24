package zel.room.games

/** A finished room game as it is stored: the game once, its teams' own points once, a line per player. */
data class GameRecord(
    val roomId: Int,
    val game: String,
    val startedAt: Int,
    val endedAt: Int,
    val teams: List<Team>,
) {
    data class Team(
        val color: Int,
        val points: Int,
        val players: List<Player>,
    )

    data class Player(
        val playerId: Int,
        val score: Int,
    )
}
