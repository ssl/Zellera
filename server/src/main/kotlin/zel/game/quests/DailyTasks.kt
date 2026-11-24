package zel.game.quests
import zel.core.Configuration
import zel.core.DailyRound
import zel.core.rules.rules
import zel.game.achievements.ExploreActivityAchievements
import zel.game.players.Player
import zel.game.players.PlayerSaveQueue
import zel.game.players.PlayerStates
import zel.protocol.quests.DailyTaskMessages
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import javax.sql.DataSource

/**
 * Daily tasks per player and daily round (the task "day"); completed ones stay claimable, the bonus opens once the
 * regulars are done.
 */
class DailyTasks private constructor(
    private val records: DailyTaskRecords,
    tasksPerDay: () -> Int,
    writes: PlayerSaveQueue,
    private val completeDays: DailyTaskStreaks.CompleteDays,
    private val unixTime: () -> Int,
    private val messages: DailyTaskMessages,
    private val rewards: QuestRewards,
    private val explore: ExploreActivityAchievements,
    private val round: DailyRound = DailyTaskListing.WHOLE_DAYS,
) {
    constructor(
        dataSource: DataSource,
        configuration: Configuration,
        persistence: (Runnable) -> Unit,
        unixTime: () -> Int,
        messages: DailyTaskMessages,
        rewards: QuestRewards,
        explore: ExploreActivityAchievements,
    ) : this(
        DailyTaskTable(dataSource),
        { configuration.getInt(PER_DAY_SETTING, PER_DAY_DEFAULT) },
        PlayerSaveQueue(persistence),
        DailyTaskStreaks.Stored(dataSource),
        unixTime,
        messages,
        rewards,
        explore,
        DailyRound.of(configuration),
    )

    /** Without a database: tasks come from [add], nothing is stored. */
    internal constructor(
        messages: DailyTaskMessages,
        rewards: QuestRewards,
        explore: ExploreActivityAchievements,
        unixTime: () -> Int = { Instant.now().epochSecond.toInt() },
        round: DailyRound = DailyTaskListing.WHOLE_DAYS,
    ) : this(
        DailyTaskRecords.None,
        { PER_DAY_DEFAULT },
        PlayerSaveQueue { it.run() },
        DailyTaskStreaks.InMemory(),
        unixTime,
        messages,
        rewards,
        explore,
        round,
    )

    private val roster = DailyTaskRoster(tasksPerDay)
    private val boards = PlayerStates<DailyTaskBoard>(writes, { it.day == today() }, ::boardFor)

    init {
        reload()
    }

    fun reload() = roster.replaceWith(records.taskList())

    fun add(task: DailyTask) = roster.add(task)

    val tasks: List<DailyTask> get() = roster.all

    fun pickFor(playerId: Int): List<DailyTask> = pickFor(playerId, today())

    fun pickFor(
        playerId: Int,
        day: LocalDate,
    ): List<DailyTask> = roster.pick(playerId, day)

    internal fun boardOf(playerId: Int): DailyTaskBoard = boards.of(playerId)

    fun unload(playerId: Int) = boards.unload(playerId)

    /** The player's cards: today's pick (the bonus once open) and earlier days' completed, unclaimed tasks. */
    fun cards(player: Player): List<DailyTaskCard> {
        if (!rules.dailyTasks) return emptyList()
        return boards.change(player.profile.id) { PlayerDay(it).cards() }
    }

    fun showCards(player: Player) {
        val client = player.client ?: return
        messages.active(client, cards(player))
    }

    fun claimableCount(player: Player): Int = cards(player).count { it.status == DailyTaskStatus.COMPLETED }

    fun countGoal(
        player: Player,
        goal: CountedAction,
        amount: Int,
    ) = count(player, DailyTask.Event.Goal(goal), amount)

    fun countHandItem(
        player: Player,
        handItem: Int,
    ) = count(player, DailyTask.Event.HandItem(handItem), 1)

    fun countPetChoice(
        player: Player,
        petType: Int,
    ) = count(player, DailyTask.Event.PetSelected(petType), 1)

    fun countFurniUse(
        player: Player,
        action: FurniAction,
        furniClass: String,
    ) = count(player, DailyTask.Event.Furni(action, furniClass.lowercase(Locale.ROOT)), 1)

    fun countPulse(
        player: Player,
        postures: Set<RoomPosture>,
    ) = count(player, DailyTask.Event.Pulse(postures), 1)

    fun claim(
        player: Player,
        listedId: Int,
    ): Boolean {
        if (!rules.dailyTasks || !DailyTaskListing.isListedId(listedId)) return false
        val day = DailyTaskListing.dayOf(listedId)
        val task = roster[DailyTaskListing.taskIdOf(listedId)] ?: return false
        val playerId = player.profile.id
        val claimed =
            boards.change(playerId) { board ->
                val pickedThatDay = day != board.day || task in pickFor(playerId, board.day)
                board.takeIf { pickedThatDay }?.claim(task.id, day)?.also { write(playerId, it) }
            } ?: return false
        val now = instant()
        val seconds =
            if (day == today()) {
                DailyTaskListing.secondsLeftToday(day, now, round)
            } else {
                DailyTaskListing.secondsPastEnd(day, now, round)
            }
        player.client?.let { messages.updated(it, listedId.toLong(), claimed.repeats, claimed.status.code, seconds) }
        rewards.grant(player, task.reward.toQuestReward())
        if (!task.isBonus) PlayerActions.claimedDailyTask(player)
        return true
    }

    private fun count(
        player: Player,
        event: DailyTask.Event,
        amount: Int,
    ) {
        if (!rules.dailyTasks || amount <= 0 || roster.isEmpty) return
        boards.change(player.profile.id) { PlayerDay(it).count(player, event, amount) }
    }

    private inner class PlayerDay(
        val board: DailyTaskBoard,
    ) {
        val pick = pickFor(board.playerId, board.day)
        var bonusOpen = regularsComplete()

        /** Every matching task takes [amount]; completing the last regular one opens the bonus for the rest. */
        fun count(
            player: Player,
            event: DailyTask.Event,
            amount: Int,
        ) {
            for (task in pick) {
                if (!task.matches(event) || (task.isBonus && !bonusOpen)) continue
                val completed = step(player, task, amount)
                if (completed && !task.isBonus && !bonusOpen && regularsComplete()) {
                    bonusOpen = true
                    openBonus(player)
                }
            }
        }

        private fun step(
            player: Player,
            task: DailyTask,
            amount: Int,
        ): Boolean {
            val moved = board.count(task, amount) ?: return false
            write(board.playerId, moved)
            val id = DailyTaskListing.listedId(task, board.day).toLong()
            player.client?.let { messages.updated(it, id, moved.repeats, moved.status.code, secondsLeft()) }
            if (moved.isClaimable) explore.dailyTaskCompleted(player)
            return moved.isClaimable
        }

        private fun openBonus(player: Player) {
            explore.dailyTaskStreak(player, DailyTaskStreaks.completed(completeDays, board.playerId, board.day))
            val bonus = pick.filter { it.isBonus }.map(::card)
            if (bonus.isNotEmpty()) player.client?.let { messages.added(it, bonus) }
        }

        fun cards(): List<DailyTaskCard> {
            val now = instant()
            val earlier =
                board.earlierClaimable().mapNotNull { done ->
                    val task = roster[done.taskId] ?: return@mapNotNull null
                    DailyTaskListing.card(task, done.day, done, DailyTaskListing.secondsPastEnd(done.day, now, round))
                }
            return pick.filter { !it.isBonus || bonusOpen }.map(::card) + earlier
        }

        private fun card(task: DailyTask) = DailyTaskListing.card(task, board.day, board[task.id], secondsLeft())

        private fun regularsComplete() = DailyTaskStreaks.dayComplete(pick) { board.isDone(it) }

        private fun secondsLeft() = DailyTaskListing.secondsLeftToday(board.day, instant(), round)
    }

    private fun write(
        playerId: Int,
        progress: DailyTaskProgress,
    ) {
        val now = unixTime()
        boards.write(playerId) { records.record(playerId, progress, now) }
    }

    private fun boardFor(
        playerId: Int,
        outdated: DailyTaskBoard?,
    ): DailyTaskBoard {
        val board = DailyTaskBoard(playerId, today())
        records.dayRows(playerId, board.day).forEach(board::restore)
        records.leftoverClaims(playerId, board.day).forEach(board::restoreEarlier)
        outdated?.let(board::takeOverFrom)
        return board
    }

    private fun instant(): Instant = Instant.ofEpochSecond(unixTime().toLong())

    private fun today(): LocalDate = DailyTaskListing.dayAt(instant(), round)

    companion object {
        const val PULSE_SECONDS = 3
        private const val PER_DAY_SETTING = "daily_tasks.per_day"
        private const val PER_DAY_DEFAULT = 3
    }
}
