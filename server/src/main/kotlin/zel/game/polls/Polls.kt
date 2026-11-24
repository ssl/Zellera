package zel.game.polls

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.game.players.Player
import java.util.concurrent.ConcurrentHashMap

class Polls internal constructor(
    private val store: PollStore,
) {
    /** Replaced whole by a reload; a poll made after it is added on first use. */
    @Volatile
    private var cached = ConcurrentHashMap<Int, Poll>()

    fun donePoll(
        player: Player,
        pollId: Int,
    ): Boolean =
        reading(false, "read the poll answers of user ${player.profile.id}") {
            store.hasAnswered(player.profile.id, pollId)
        }

    fun declinedPoll(
        player: Player,
        pollId: Int,
    ): Boolean =
        reading(false, "read the poll answers of user ${player.profile.id}") {
            store.hasDeclined(player.profile.id, pollId)
        }

    fun completedPoll(
        player: Player,
        poll: Poll,
    ): Boolean =
        reading(false, "read the poll answers of user ${player.profile.id}") {
            poll.isComplete(store.answeredQuestions(player.profile.id, poll.id))
        }

    fun pollToOffer(player: Player): Poll? =
        reading(null, "find a poll for user ${player.profile.id}") {
            store.firstOpenPoll(player.profile.id)?.let(::getPoll)
        }

    fun reload() {
        val loaded = reading(null, "load the polls") { store.polls() } ?: return
        cached = ConcurrentHashMap(loaded)
    }

    fun getPoll(pollId: Int): Poll? {
        if (pollId <= 0) return null
        return cached[pollId] ?: reading(null, "load poll $pollId") { store.poll(pollId) }?.also { cached[pollId] = it }
    }

    private inline fun <T> reading(
        fallback: T,
        what: String,
        read: () -> T,
    ): T =
        try {
            read()
        } catch (e: SqlFailure) {
            LOGGER.error("Could not {}", what, e)
            fallback
        }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(Polls::class.java)
    }
}
