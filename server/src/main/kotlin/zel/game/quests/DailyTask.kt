package zel.game.quests

import org.slf4j.LoggerFactory
import java.util.Locale

data class DailyTask(
    val id: Int,
    val code: String,
    val goal: QuestGoal,
    val isBonus: Boolean,
    val reward: StoredReward,
    val catalogName: String,
    val imageVersion: String,
    val sortOrder: Int,
) {
    data class StoredReward(
        val type: String,
        val extra: String,
        val amount: Int,
    ) {
        fun toQuestReward(): QuestReward = QuestReward.of(type, extra, amount)
    }

    sealed interface Event {
        /** A bare goal: only tasks without a target list move on it. */
        data class Goal(
            val type: CountedAction,
        ) : Event

        data class HandItem(
            val id: Int,
        ) : Event

        data class PetSelected(
            val type: Int,
        ) : Event

        data class Furni(
            val action: FurniAction,
            val furniClass: String,
        ) : Event

        data class Pulse(
            val postures: Set<RoomPosture>,
        ) : Event
    }

    val requiredRepeats: Int get() = goal.count

    val listedType: String get() = goal.type.name

    val targetNames: Set<String> by lazy {
        goal.data
            .split(",")
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter(String::isNotEmpty)
            .toSet()
    }

    /** The target names as ids; empty, with a warning, as soon as one of them is no number. */
    val targetIds: Set<Int> by lazy {
        val ids = targetNames.mapNotNull(String::toIntOrNull)
        if (ids.size == targetNames.size) {
            ids.toSet()
        } else {
            LOGGER.warn("Daily task {} has goal data that lists no ids: {}", id, goal.data)
            setOf()
        }
    }

    val hasTargets: Boolean get() = goal.type !in TARGET_BOUND_GOALS || targetNames.isNotEmpty()

    fun matches(event: Event): Boolean =
        when (event) {
            is Event.Goal -> event.type !in TARGET_BOUND_GOALS && goal.type == event.type
            is Event.HandItem -> goal.type == CountedAction.FIND_HAND_ITEM && event.id in targetIds
            is Event.PetSelected -> goal.type == CountedAction.FIND_PET && event.type in targetIds
            is Event.Furni -> goal.type == event.action.goal && event.furniClass in targetNames
            is Event.Pulse -> goal.type == CountedAction.ROOM_PULSE && event.postures.any { it.goalName in targetNames }
        }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DailyTask::class.java)

        /** Goals bound to a target only goal data carries: the bare goal moves no daily task of these. */
        internal val TARGET_BOUND_GOALS: Set<CountedAction> =
            setOf(
                CountedAction.FIND_STUFF,
                CountedAction.CLICK_FURNI,
                CountedAction.WALK_OVER_STUFF,
                CountedAction.FIND_HAND_ITEM,
                CountedAction.FIND_PET,
                CountedAction.ROOM_PULSE,
            )
    }
}
