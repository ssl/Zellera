package zel.room.games

data class GameResult(
    val gameName: String,
    val teams: List<TeamResult>,
    val seconds: Int = 0,
)
