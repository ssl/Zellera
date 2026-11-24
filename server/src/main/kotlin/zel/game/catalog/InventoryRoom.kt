package zel.game.catalog

import zel.core.Configuration
import zel.core.Texts
import zel.game.bots.BotLimits
import zel.game.items.ProductKind
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.players.inventory.Inventory

/** An order is refused when what it would add does not fit: the caps count what is delivered, not what is held. */
class InventoryRoom(
    private val config: Configuration,
    private val texts: Texts,
) {
    data class Load(
        val furni: Int,
        val bots: Int,
        val pets: Int,
    )

    /** The alert for the first inventory [load] does not fit in, or null. */
    fun lackFor(
        player: Player,
        load: Load,
    ): String? {
        val inventory = player.inventory
        val petLimit = config.getInt(PET_LIMIT, DEFAULT_PET_LIMIT)
        return when {
            load.furni > 0 && !fits(inventory.furni.count, load.furni, Inventory.maximumItems) -> {
                texts.text(FURNI_FULL)
            }

            load.bots > 0 && !player.hasPermission(Privilege.BOTS_UNLIMITED) &&
                !fits(inventory.bots.count, load.bots, BotLimits.maxInventory) -> {
                texts.text(BOTS_FULL).replace(LIMIT, BotLimits.maxInventory.toString())
            }

            load.pets > 0 && !player.hasPermission(Privilege.PETS_UNLIMITED) &&
                !fits(inventory.pets.count, load.pets, petLimit) -> {
                texts.text(PETS_FULL).replace(LIMIT, petLimit.toString())
            }

            else -> {
                null
            }
        }
    }

    companion object {
        const val FURNI_FULL = "inventory.furni.full"
        private const val BOTS_FULL = "inventory.bots.full"
        private const val PETS_FULL = "inventory.pets.full"
        private const val LIMIT = "%limit%"
        private const val PET_LIMIT = "player.inventory.max_pets"
        private const val DEFAULT_PET_LIMIT = 25

        fun fits(
            held: Int,
            added: Int,
            limit: Int,
        ): Boolean = held.toLong() + added <= limit

        fun loadOf(
            offer: CatalogOffer,
            units: Int,
        ): Load {
            var furni = 0L
            var bots = 0L
            var pets = 0L
            offer.products.forEach { product ->
                val copies = offer.quantityOf(product.id).toLong() * units
                when {
                    product.isBot -> bots += copies
                    product.isPet -> pets += copies
                    product.type == ProductKind.FLOOR_FURNI || product.type == ProductKind.WALL_FURNI -> furni += copies
                }
            }
            return Load(capped(furni), capped(bots), capped(pets))
        }

        private fun capped(count: Long): Int = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
