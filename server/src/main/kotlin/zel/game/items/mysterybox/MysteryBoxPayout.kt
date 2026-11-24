package zel.game.items.mysterybox

import zel.db.Sql
import zel.game.items.ItemServices
import zel.game.players.Player
import kotlin.random.asKotlinRandom

/** Answers the prize's sprite id, 0 when the colour has no pool. */
class MysteryBoxPayout(
    private val services: ItemServices,
) {
    private data class Ticket(
        val offer: Int,
        val weight: Int,
    )

    fun grant(
        player: Player,
        colour: String,
    ): Int {
        val tickets = WeightedDraw(ticketsFor(colour).map { it.offer to it.weight })
        val won = tickets.draw(services.random().asKotlinRandom()) ?: return 0
        val offer = services.catalog().offer(won) ?: return 0
        val delivered =
            offer.products.mapNotNull { product ->
                services.items().newFurni(player.profile.id, product, 0, 0, offer.productExtraParam(product))
            }
        delivered.forEach(player.inventory.furni::deliver)
        return offer.products.firstOrNull()?.spriteId ?: 0
    }

    private fun ticketsFor(colour: String): List<Ticket> =
        Sql(
            services.dataSource,
        ).select("SELECT offer_id, weight FROM mystery_box_prizes WHERE colour IN (?, '')") {
            string(colour.trim().lowercase())
        }.rows { found ->
            Ticket(found.getInt("offer_id"), found.getInt("weight"))
        }
}
