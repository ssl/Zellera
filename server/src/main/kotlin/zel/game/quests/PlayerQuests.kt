package zel.game.quests
import zel.game.players.PlayerStates

/** One player's quest entries; changed under `synchronized(this)` through [PlayerStates.change]. */
internal class PlayerQuests(
    val playerId: Int,
    entries: List<QuestEntry>,
) : PlayerStates.Held() {
    private val entries = HashMap(entries.associateBy { it.quest })

    operator fun get(questId: Int): QuestEntry? = entries[questId]

    val accepted: List<QuestEntry> get() =
        entries.values
            .filter {
                it.accepted
            }.sortedWith(compareBy({ it.trackedSince }, { it.quest }))

    /** The quest accepted first: the one the tracker and the daily widget show. */
    val tracked: QuestEntry? get() = accepted.firstOrNull()

    fun put(entry: QuestEntry) {
        entries[entry.quest] = entry
    }
}
