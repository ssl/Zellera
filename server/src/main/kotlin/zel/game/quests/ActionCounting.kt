package zel.game.quests

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.players.Player
import zel.game.players.Players
import zel.game.rewardtracks.RewardTrackService

/** A failing counter is skipped. */
class ActionCounting(
    private val quests: Quests,
    private val dailyTasks: DailyTasks,
    private val rewardTracks: RewardTrackService,
    private val players: Players,
) : PlayerActions.Counter {
    override fun goal(
        player: Player?,
        goal: CountedAction,
        amount: Int,
    ) {
        val online = player.online() ?: return
        if (amount <= 0) return
        counting("quests", online) { quests.count(online, goal, amount) }
        counting("daily tasks", online) { dailyTasks.countGoal(online, goal, amount) }
        counting("reward tracks", online) { rewardTracks.progress(online, goal, amount) }
    }

    override fun goal(
        playerId: Int,
        goal: CountedAction,
        amount: Int,
    ) = goal(players.find(playerId), goal, amount)

    override fun handItem(
        player: Player?,
        item: Int,
    ) {
        val online = player.online() ?: return
        counting("daily tasks", online) { dailyTasks.countHandItem(online, item) }
    }

    override fun furni(
        player: Player?,
        action: FurniAction,
        furniName: String,
    ) {
        val online = player.online() ?: return
        counting("quests", online) { quests.count(online, action.goal, 1, QuestSubject.Furni(furniName)) }
        counting("daily tasks", online) { dailyTasks.countFurniUse(online, action, furniName) }
        counting("reward tracks", online) { rewardTracks.progress(online, action.goal, 1) }
    }

    override fun reached(
        playerId: Int,
        goal: CountedAction,
        size: Int,
    ) {
        val online = players.find(playerId).online() ?: return
        counting("quests", online) { quests.count(online, goal, 1, QuestSubject.Size(size)) }
        counting("daily tasks", online) { dailyTasks.countGoal(online, goal, 1) }
        counting("reward tracks", online) { rewardTracks.progress(online, goal, 1) }
    }

    override fun pet(
        player: Player?,
        petType: Int,
    ) {
        val online = player.online() ?: return
        counting("daily tasks", online) { dailyTasks.countPetChoice(online, petType) }
        goal(online, CountedAction.FIND_PET, 1)
    }

    override fun forget(playerId: Int) {
        quests.unload(playerId)
        dailyTasks.unload(playerId)
    }

    private fun Player?.online(): Player? = this?.takeIf { it.client != null }

    @Suppress("TooGenericExceptionCaught") // a failing counter must not break the action that triggered it
    private inline fun counting(
        counter: String,
        player: Player,
        count: () -> Unit,
    ) {
        try {
            count()
        } catch (failure: Exception) {
            LOGGER.error("The {} could not count an action of player {}", counter, player.profile.id, failure)
        }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(ActionCounting::class.java)
    }
}
