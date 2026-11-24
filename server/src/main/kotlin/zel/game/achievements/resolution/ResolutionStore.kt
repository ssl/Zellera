package zel.game.achievements.resolution

interface ResolutionStore {
    fun openAchievementIds(playerId: Int): Set<Int>

    fun byFurni(furniId: Int): Resolution?

    fun add(resolution: Resolution): Boolean

    fun delete(furniId: Int)

    fun markCompleted(
        furniId: Int,
        now: Int,
    )
}
