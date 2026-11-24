package zel.game.catalog

import zel.game.players.Player
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.LtdRaffleResult
import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap

/** Once a copy of an edition sells, the rest sell first come, first served. */
class ReleaseRaffles(
    private val services: CatalogServices,
) {
    private val running = ConcurrentHashMap<Int, ReleaseRaffle>()

    private val enabled: Boolean get() = services.config.getBoolean(ENABLED, true)

    private val windowMillis: Long
        get() = maxOf(1, services.config.getInt(WINDOW_SECONDS, DEFAULT_WINDOW_SECONDS)) * MILLIS_PER_SECOND

    fun blocks(item: CatalogOffer): Boolean = item.isLimited && enabled && unsold(services.catalog.limited.get(item))

    fun join(
        item: CatalogOffer?,
        buyer: Player,
    ): Boolean {
        if (item == null || !blocks(item)) return false
        var opened = false
        val raffle = running.computeIfAbsent(item.id) { ReleaseRaffle(item).also { opened = true } }
        if (opened) schedule(raffle)
        // a raffle the draw closed in the meantime leaves the buyer to the plain purchase
        if (!raffle.enter(buyer)) return false
        buyer.client?.let { CatalogReplies.ltdRaffleEntered(it, nameOf(item)) }
        return true
    }

    fun cancelOpen() {
        running.keys.toList().forEach { offerId ->
            val raffle = running.remove(offerId) ?: return@forEach
            val name = nameOf(raffle.item)
            raffle.cancel().forEach { player ->
                player.client?.let { CatalogReplies.ltdRaffleResult(it, name, LtdRaffleResult.CANCELLED) }
            }
        }
    }

    private fun schedule(raffle: ReleaseRaffle) {
        services.threading.run({ draw(raffle) }, windowMillis) ?: running.remove(raffle.item.id, raffle)
    }

    private fun draw(raffle: ReleaseRaffle) {
        if (!running.remove(raffle.item.id, raffle)) return
        val copies =
            services.catalog.limited
                .get(raffle.item)
                ?.available() ?: 0
        val name = nameOf(raffle.item)
        ReleaseRaffle
            .results(raffle.draw(services.random), copies) { player ->
                services.catalog.purchase(raffle.item, player, 1, "", false)
            }.forEach { (player, result) ->
                player.client?.let { CatalogReplies.ltdRaffleResult(it, name, result) }
                if (result == LtdRaffleResult.WON) LimitedEditionAchievements.wonReleaseRaffle(player)
            }
    }

    private fun nameOf(item: CatalogOffer): String =
        item.products
            .firstOrNull()
            ?.name
            .orEmpty()

    companion object {
        private const val ENABLED = "catalog.limited.raffle.is_on"
        private const val WINDOW_SECONDS = "catalog.limited.raffle.window.seconds"
        private const val DEFAULT_WINDOW_SECONDS = 10

        fun unsold(stock: LimitedEditionStock?): Boolean =
            stock != null && stock.editionSize > 0 && stock.available() == stock.editionSize
    }
}
