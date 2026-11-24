package zel.game.catalog

import zel.game.players.Player
import zel.protocol.outgoing.catalog.LtdRaffleResult
import org.slf4j.LoggerFactory
import java.util.Random
import kotlin.random.asKotlinRandom

internal class ReleaseRaffle(
    val item: CatalogOffer,
) {
    private val entrants = LinkedHashMap<Int, Player>()
    private var closed = false

    /** False once the draw has closed the raffle. */
    @Synchronized
    fun enter(player: Player): Boolean {
        if (closed) return false
        entrants.putIfAbsent(player.profile.id, player)
        return true
    }

    @Synchronized
    fun draw(random: Random): List<Player> {
        closed = true
        return entrants.values.filter { it.client != null }.shuffled(random.asKotlinRandom())
    }

    @Synchronized
    fun cancel(): List<Player> {
        closed = true
        return entrants.values.toList()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ReleaseRaffle::class.java)

        /** A refused copy goes to the next player; a throwing delivery answers ERROR. */
        fun results(
            players: List<Player?>,
            copies: Int,
            deliver: (Player) -> Boolean,
        ): List<Pair<Player, LtdRaffleResult>> {
            var left = copies
            return players.filterNotNull().map { player ->
                val result = if (left > 0) attempt(player, deliver) else LtdRaffleResult.LOST
                if (result == LtdRaffleResult.WON) left--
                player to result
            }
        }

        @Suppress("TooGenericExceptionCaught") // whatever breaks the delivery is answered as an error
        private fun attempt(
            player: Player,
            deliver: (Player) -> Boolean,
        ): LtdRaffleResult =
            try {
                if (deliver(player)) LtdRaffleResult.WON else LtdRaffleResult.LOST
            } catch (e: Exception) {
                log.error("Delivering a raffled copy to player {} broke off", player.profile.id, e)
                LtdRaffleResult.ERROR
            }
    }
}
