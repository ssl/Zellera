package zel.game.games.snowball

import zel.protocol.snowball.SnowballPacket
import java.util.concurrent.ScheduledFuture
import java.util.random.RandomGenerator

@Suppress("TooManyFunctions") // the lobby's whole view of the server
interface SnowballRuntime {
    fun settings(): SnowballSettings

    /** A monotonic millisecond clock for timers and turn pacing; only differences between readings mean anything. */
    fun nowMillis(): Long

    fun siteUrl(): String

    fun schedule(
        task: Runnable,
        delayMillis: Long,
    ): ScheduledFuture<*>?

    fun send(
        userId: Int,
        packet: SnowballPacket<*>,
    )

    /** Leaves the room without the landing view and returns its id, or -1; the session drops on Game2EnterArena. */
    fun leaveRoomForGame(userId: Int): Int

    fun skill(userId: Int): SnowballSkill

    fun gamesPlayed(userId: Int): Int

    fun allowance(userId: Int): SnowballAllowance

    /** Counts a game that starts against the day's free games, or else takes a token. */
    fun recordPlay(userId: Int)

    fun random(): RandomGenerator

    fun filterChat(
        userId: Int,
        text: String,
    ): String?

    fun recordScores(scoresByUserId: Map<Int, Int>)

    fun progressAchievements(awards: List<SnowballWeekly.AchievementAward>)

    fun notifyFriends(
        userId: Int,
        type: Int,
    )

    fun settleLastWeek()

    /** Whether the user's client last reported its window without the focus: such a window may get no frames. */
    fun isInBackground(userId: Int): Boolean
}
