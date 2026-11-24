package zel.game.games.snowball

interface SnowballPlayStore {
    fun playedToday(playerId: Int): Int

    fun recordPlay(playerId: Int)

    fun tokens(playerId: Int): Int

    fun addTokens(
        playerId: Int,
        games: Int,
    ): Boolean

    fun takeToken(playerId: Int): Boolean
}
