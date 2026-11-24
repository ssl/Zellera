package zel.game.quests

import zel.game.players.Player

/**
 * Counts off the caller's thread, in order per player: counting may read or write the database (first loads,
 * payouts), and the callers include room ticks that hold their room's lock. A count runs for the player online when
 * it runs, so nothing counts into a session that ended meanwhile.
 */
class OrderedCounting(
    private val counter: PlayerActions.Counter,
    private val ordered: (Any, Runnable) -> Unit,
    private val online: (Int) -> Player?,
) : PlayerActions.Counter {
    private data class Lane(
        val playerId: Int,
    )

    private fun later(
        playerId: Int,
        job: () -> Unit,
    ) = ordered(Lane(playerId), Runnable(job))

    override fun goal(
        player: Player?,
        goal: CountedAction,
        amount: Int,
    ) {
        val id = player?.profile?.id ?: return
        later(id) { counter.goal(online(id), goal, amount) }
    }

    override fun goal(
        playerId: Int,
        goal: CountedAction,
        amount: Int,
    ) = later(playerId) { counter.goal(playerId, goal, amount) }

    override fun furni(
        player: Player?,
        action: FurniAction,
        furniName: String,
    ) {
        val id = player?.profile?.id ?: return
        later(id) { counter.furni(online(id), action, furniName) }
    }

    override fun reached(
        playerId: Int,
        goal: CountedAction,
        size: Int,
    ) = later(playerId) { counter.reached(playerId, goal, size) }

    override fun handItem(
        player: Player?,
        item: Int,
    ) {
        val id = player?.profile?.id ?: return
        later(id) { counter.handItem(online(id), item) }
    }

    override fun pet(
        player: Player?,
        petType: Int,
    ) {
        val id = player?.profile?.id ?: return
        later(id) { counter.pet(online(id), petType) }
    }

    /** At once: a count still queued finds the player offline and does nothing. */
    override fun forget(playerId: Int) = counter.forget(playerId)
}
