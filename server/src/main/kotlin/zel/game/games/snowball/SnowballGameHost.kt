package zel.game.games.snowball

import zel.core.Configuration
import zel.core.rules.rules
import zel.core.threading.GameThreads
import zel.game.achievements.Achievements
import zel.game.achievements.GameEvents
import zel.game.achievements.countFor
import zel.game.earnings.IncomeRewards
import zel.game.items.Items
import zel.game.messenger.FriendUpdates
import zel.game.moderation.WordFilter
import zel.game.players.Player
import zel.game.players.Players
import zel.protocol.misc.ClientWindow
import zel.protocol.snowball.SnowballPacket
import zel.util.NANOS_PER_MILLI
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import java.util.random.RandomGenerator

private val log = LoggerFactory.getLogger(SnowballGameHost::class.java)

internal class SnowballGameHost(
    private val scores: SnowballScoreTable,
    private val plays: SnowballPlayStore,
    clock: Clock,
    private val config: Configuration,
    private val threads: GameThreads,
    private val players: Players,
    private val achievements: Achievements,
    private val chatFilter: WordFilter,
    private val items: Items,
    private val income: IncomeRewards,
    private val friendUpdates: Supplier<FriendUpdates>,
    private val siteAddress: Supplier<String>,
) : SnowballRuntime,
    SnowballWeekly.Effects {
    private val weekly = SnowballWeekly(scores, this, clock)
    private val settleRunning = AtomicBoolean()

    override fun settings(): SnowballSettings = SnowballSettings.from(config)

    override fun nowMillis(): Long = System.nanoTime() / NANOS_PER_MILLI

    override fun siteUrl(): String = siteAddress.get()

    override fun schedule(
        task: Runnable,
        delayMillis: Long,
    ): ScheduledFuture<*>? = threads.run(task, delayMillis)

    private fun online(playerId: Int): Player? = players.find(playerId)

    override fun send(
        userId: Int,
        packet: SnowballPacket<*>,
    ) {
        online(userId)?.client?.let { packet.sendTo(it) }
    }

    override fun leaveRoomForGame(userId: Int): Int = online(userId)?.rooms?.leaveForGame(userId) ?: -1

    override fun skill(userId: Int): SnowballSkill {
        val player = online(userId) ?: return SnowballSkill.NONE
        val definition =
            achievements.forEvent(GameEvents.GAME_SNOWBALL_TOTAL_SCORE).firstOrNull() ?: return SnowballSkill.NONE
        return SnowballSkill.of(definition, player.stats.progressOf(definition))
    }

    override fun gamesPlayed(userId: Int): Int = scores.gamesPlayed(userId)

    override fun allowance(userId: Int): SnowballAllowance =
        SnowballAllowance(
            plays.playedToday(userId),
            plays.tokens(userId),
            online(userId)?.stats?.club?.isMember == true,
        )

    /** Off the game's timer, like the scores: the allowance read and the write must not hold up the lobby's lock. */
    override fun recordPlay(userId: Int) {
        threads.write { if (allowance(userId).nextIsFree(rules)) plays.recordPlay(userId) else plays.takeToken(userId) }
    }

    override fun random(): RandomGenerator = ThreadLocalRandom.current()

    /** Null when the player may not talk or the text is blocked outright; otherwise the text with bad words covered. */
    override fun filterChat(
        userId: Int,
        text: String,
    ): String? {
        val player = online(userId)?.takeIf { it.stats.standing.mayTalk() } ?: return null
        return if (chatFilter.hides(text)) null else chatFilter.filter(text, player)
    }

    /** Off the game's timer: the database write must not hold up the lobby's lock. */
    override fun recordScores(scoresByUserId: Map<Int, Int>) {
        val frozen = scoresByUserId.toMap()
        threads.write { scores.recordScores(frozen) }
    }

    override fun progressAchievements(awards: List<SnowballWeekly.AchievementAward>) {
        val frozen = awards.toList()
        threads.write { frozen.forEach { progressAchievement(it.userId, it.event, it.amount) } }
    }

    override fun progressAchievement(
        userId: Int,
        event: String,
        amount: Int,
    ) = achievements.countFor(userId, event, amount)

    override fun reward(
        userId: Int,
        type: Int,
        amount: Int,
        productCode: String,
    ): Boolean = income.add(userId, IncomeRewards.SNOWBALL, type, amount, productCode)

    override fun furniExists(classname: String): Boolean = items.getItem(classname) != null

    override fun notifyFriends(
        userId: Int,
        type: Int,
    ) {
        val friendIds = online(userId)?.messenger?.friends?.keys ?: return
        for (friendId in friendIds) {
            val friend = online(friendId)?.takeIf { it.client != null } ?: continue
            friendUpdates.get().notifyFriend(friend, userId, type, SnowballLobby.FRIEND_BAR_GAME)
        }
    }

    override fun isInBackground(userId: Int): Boolean =
        online(userId)?.client?.let { ClientWindow.isInBackground(it) } == true

    /** One settle at a time, off the caller's thread; a failed settle is logged and the next request tries again. */
    @Suppress("TooGenericExceptionCaught") // a failed settle must let the next request try again
    override fun settleLastWeek() {
        if (!settleRunning.compareAndSet(false, true)) return
        try {
            val prizes = SnowballWeeklyPrizes.from(config)
            threads.write {
                try {
                    weekly.settleLastWeek(prizes)
                } catch (e: RuntimeException) {
                    log.error("Snowball: the weekly results could not be settled.", e)
                } finally {
                    settleRunning.set(false)
                }
            }
        } catch (e: RuntimeException) {
            // prizes unreadable or not handed to the executor (shutting down): the next request tries again
            settleRunning.set(false)
            log.warn("Snowball: the weekly results could not be scheduled.", e)
        }
    }
}
