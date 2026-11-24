package zel.game.players.level

interface AccountLevelStore {
    fun read(playerId: Int): AccountStanding?

    fun inputs(playerId: Int): LevelInputs

    fun save(standing: AccountStanding)

    fun addHosting(
        playerId: Int,
        levelMinutes: Long,
    )
}
