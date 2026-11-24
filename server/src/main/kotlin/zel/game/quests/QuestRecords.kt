package zel.game.quests

interface QuestRecords {
    fun definitions(): List<Quest>

    fun rowsOf(playerId: Int): List<QuestEntry>

    fun store(
        playerId: Int,
        entry: QuestEntry,
    )
}
