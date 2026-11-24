package zel.game.quests

internal class QuestBook(
    all: List<Quest>,
) {
    class Campaign(
        val code: String,
        val quests: List<Quest>,
    ) {
        val isTimed: Boolean get() = quests.first().hasDeadline
    }

    private val byId: Map<Int, Quest> = all.associateBy(Quest::id)

    private val campaigns: Map<String, Campaign> =
        all
            .filterNot(Quest::isDaily)
            .groupBy { it.slot.campaign }
            .mapValues { (code, quests) -> Campaign(code, quests.sortedWith(READING_ORDER)) }

    val ongoing: List<Campaign> = campaigns.values.filterNot(Campaign::isTimed)

    val timed: List<Campaign> = campaigns.values.filter(Campaign::isTimed)

    val dailies: List<Quest> = all.filter(Quest::isDaily).sortedWith(READING_ORDER)

    val isEmpty: Boolean get() = byId.isEmpty()

    operator fun get(id: Int): Quest? = byId[id]

    operator fun contains(id: Int): Boolean = id in byId

    fun campaign(code: String): Campaign? = campaigns[code]

    fun campaignOf(quest: Quest): Campaign? = campaigns[quest.slot.campaign]

    private companion object {
        val READING_ORDER = compareBy<Quest>({ it.slot.position }, { it.id })
    }
}
