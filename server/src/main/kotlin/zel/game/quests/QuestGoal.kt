package zel.game.quests

import java.util.Locale

/** What a quest or task counts: [count] actions of [type]; [data] lists target furni names or the least size. */
data class QuestGoal(
    val type: CountedAction,
    val data: String,
    val count: Int,
) {
    private val names: Set<String> by lazy {
        data
            .split(',')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter(String::isNotEmpty)
            .toSet()
    }

    private val leastSize: Int? by lazy { data.trim().toIntOrNull() }

    /** A goal without data takes every report of its type; one with data only a report that fits it. */
    fun admits(subject: QuestSubject?): Boolean =
        when {
            data.isBlank() -> true
            subject is QuestSubject.Furni -> subject.name.lowercase(Locale.ROOT) in names
            subject is QuestSubject.Size -> leastSize?.let { subject.amount >= it } == true
            else -> false
        }
}
