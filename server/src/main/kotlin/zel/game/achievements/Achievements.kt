package zel.game.achievements

import zel.game.players.Player
import zel.game.players.Players
import zel.game.players.WalletWrites
import zel.game.players.inventory.BadgeCaches
import zel.game.players.level.AccountLevels
import zel.protocol.achievements.AchievementMessages
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class Achievements(
    dataSource: DataSource,
    private val players: Players,
    private val outbox: AchievementMessages,
    schedule: (Runnable, Long) -> Unit,
    accountLevels: AccountLevels?,
    badgeCaches: BadgeCaches,
) : AchievementProgress {
    private val store = AchievementStore(dataSource)
    private val book = AchievementBook({ store.loadAll(this) }, store::loadTriggers)
    private val steps =
        AchievementSteps(dataSource, outbox, LevelUpAnnouncer(outbox, accountLevels, badgeCaches), badgeCaches)
    private val heldBack = HeldBackSteps(schedule, ::heldBackDue)

    val all: Map<String, Achievement> get() = book.all

    override fun forEvent(event: String): List<Achievement> = book.forEvent(event)

    /** The achievement with this code in the content pack, for rows and commands that name it. */
    fun named(code: String): Achievement? = book[code]

    fun getAchievement(id: Int): Achievement? = book.withId(id)

    internal fun add(achievement: Achievement) = book.add(achievement)

    internal fun addTriggers(triggers: List<AchievementTrigger>) = book.addTriggers(triggers)

    fun refresh() = book.refresh()

    override fun progressFor(
        playerId: Int,
        achievement: Achievement?,
        amount: Int,
    ) {
        if (achievement == null || amount <= 0 || playerId <= 0) return
        // a player still logging in or already logging out is in the map but not online
        val player = players.find(playerId)
        if (player?.isOnline == true) {
            progress(player, achievement, amount)
        } else if (AchievementProgression.counts(achievement)) {
            store.countAway(playerId, achievement, amount)
        }
    }

    override fun progress(
        player: Player?,
        achievement: Achievement?,
        amount: Int,
    ) {
        if (achievement == null || player == null || !player.isOnline || amount <= 0) return
        when {
            achievement.countsOn(HeldBackSteps.HELD_BACK_EVENT) -> heldBack.add(player, achievement, amount)
            else -> step(player, achievement, amount)
        }
    }

    override fun announce(
        player: Player,
        achievement: Achievement,
    ) {
        val client = player.client ?: return
        outbox.progress(client, AchievementEntries.entry(player, achievement))
    }

    /** At login: the session learns what was counted while the player was away, and the levels owed are handed out. */
    fun settleStanding(player: Player) {
        guarded("settle the achievements", player) {
            for ((achievementId, standing) in store.standing(player.profile.id)) {
                val achievement = book.withId(achievementId) ?: continue
                player.stats.recordProgress(achievement, standing.progress)
                val owed = AchievementProgression.owed(achievement, standing.progress, standing.rewardedLevel)
                if (owed.isNotEmpty()) steps.settle(player, achievement, 0)
            }
        }
    }

    fun playerLeft(player: Player) {
        val playerId = player.profile.id
        heldBack.drain(player).forEach { (achievement, amount) -> store.countAway(playerId, achievement, amount) }
    }

    private fun heldBackDue(
        player: Player,
        achievement: Achievement,
        amount: Int,
    ) {
        if (player.isOnline) {
            step(player, achievement, amount)
        } else {
            store.countAway(player.profile.id, achievement, amount)
        }
    }

    private fun step(
        player: Player,
        achievement: Achievement,
        amount: Int,
    ) = guarded("progress achievement ${achievement.name}", player) { steps.count(player, achievement, amount) }

    /** Under the player's wallet lock; a failure is logged and goes no further. */
    @Suppress("TooGenericExceptionCaught") // a failing step must not break the action that progressed it
    private fun guarded(
        reason: String,
        player: Player,
        block: () -> Unit,
    ) {
        try {
            WalletWrites.locked(player) { block() }
        } catch (e: Exception) {
            LOGGER.error("Unable to {} for player {}", reason, player.profile.id, e)
        }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(Achievements::class.java)
    }
}
