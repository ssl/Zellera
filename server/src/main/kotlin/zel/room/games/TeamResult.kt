package zel.room.games

data class TeamResult(
    val color: TeamColor,
    val playerIds: List<Int>,
    val score: Int,
    val won: Boolean,
)
