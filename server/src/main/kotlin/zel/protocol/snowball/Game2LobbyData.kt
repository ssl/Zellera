package zel.protocol.snowball

import zel.game.games.snowball.SnowballMode

@JvmRecord
data class Game2LobbyData(
    val gameId: Int,
    val levelName: String,
    val gameType: Int,
    val fieldType: Int,
    val numberOfTeams: Int,
    val maximumPlayers: Int,
    val owningPlayerName: String,
    val levelEntryId: Int,
    val players: List<Player>,
    val mode: SnowballMode,
) {
    @JvmRecord
    data class Player(
        val userId: Int,
        val name: String,
        val figure: String,
        val gender: String,
        val teamId: Int,
        val skillLevel: Int,
        val totalScore: Int,
        val scoreToNextLevel: Int,
    )
}
