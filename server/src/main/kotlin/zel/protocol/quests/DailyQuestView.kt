package zel.protocol.quests

data class DailyQuestView(
    val quest: QuestView?,
    val easyOpenCount: Int,
    val hardOpenCount: Int,
)
