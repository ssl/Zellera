package zel.game.quests

sealed interface QuestSubject {
    data class Furni(
        val name: String,
    ) : QuestSubject

    data class Size(
        val amount: Int,
    ) : QuestSubject
}
