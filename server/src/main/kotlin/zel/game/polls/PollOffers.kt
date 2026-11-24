package zel.game.polls

import zel.core.threading.GameThreads
import zel.game.players.Player
import zel.protocol.outgoing.polls.PollOffer
import java.util.Collections
import java.util.WeakHashMap

/** Offers a poll 10 s after a room entry once the session is old enough; repeats until answered or declined. */
class PollOffers(
    private val polls: Polls,
    private val threading: GameThreads,
    private val now: () -> Int,
) {
    private val offered: MutableMap<Player, MutableSet<Int>> = Collections.synchronizedMap(WeakHashMap())

    fun roomEntered(
        player: Player?,
        roomId: Int,
    ) {
        if (player == null || roomId <= 0) return
        if (!offersAt(now(), player.stats.sessionStartedAt)) return

        val entry = player.rooms.unitIndex(player.profile.id) ?: return
        threading.run({ offer(player, roomId, entry) }, OFFER_DELAY_MILLIS)
    }

    private fun offer(
        player: Player,
        roomId: Int,
        entry: Int,
    ) {
        if (!sameEntry(player, roomId, entry)) return
        val poll = polls.pollToOffer(player)
        if (poll == null || !sameEntry(player, roomId, entry)) return

        markOffered(player, poll.id)
        player.client?.send(PollOffer(poll))
    }

    fun wasOffered(
        player: Player?,
        pollId: Int,
    ): Boolean = player != null && pollId > 0 && offered[player]?.contains(pollId) == true

    internal fun markOffered(
        player: Player,
        pollId: Int,
    ) {
        offered.computeIfAbsent(player) { Collections.synchronizedSet(HashSet()) }.add(pollId)
    }

    companion object {
        const val OFFER_DELAY_MILLIS = 10_000L

        const val MIN_ONLINE_SECONDS = 300

        fun offersAt(
            enteredAt: Int,
            sessionStartedAt: Int,
        ): Boolean = enteredAt - sessionStartedAt >= MIN_ONLINE_SECONDS

        fun sameEntry(
            player: Player,
            roomId: Int,
            entry: Int,
        ): Boolean =
            player.isOnline &&
                player.client != null &&
                player.rooms.roomOf(player.profile.id) == roomId &&
                player.rooms.unitIndex(player.profile.id) == entry
    }
}
