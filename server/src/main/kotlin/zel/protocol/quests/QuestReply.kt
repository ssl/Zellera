package zel.protocol.quests

sealed interface QuestReply {
    data class Tracked(
        val quest: QuestView,
    ) : QuestReply

    data class Stopped(
        val expired: Boolean,
        val quest: QuestView,
    ) : QuestReply

    data class Completed(
        val quest: QuestView,
        val showDialog: Boolean,
    ) : QuestReply
}
