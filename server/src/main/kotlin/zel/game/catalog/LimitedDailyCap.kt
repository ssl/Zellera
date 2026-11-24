package zel.game.catalog

import zel.core.Configuration
import zel.core.Texts
import zel.game.players.Player

/** Limited copies per player per day, overall and per offer; 0 means no cap. */
class LimitedDailyCap(
    private val config: Configuration,
    private val texts: Texts,
) {
    fun reached(
        player: Player,
        offer: CatalogOffer,
    ): String? {
        if (!config.getBoolean(ENABLED, true)) return null
        val bought = player.stats.limitedPurchases
        val overall = config.getInt(OVERALL, 0)
        val perOffer = config.getInt(PER_OFFER, 0)
        return when {
            overall > 0 && bought.total() >= overall -> {
                texts.text(OVERALL_TEXT).replace("%limit%", overall.toString())
            }

            perOffer > 0 && bought.total(offer.id) >= perOffer -> {
                texts
                    .text(PER_OFFER_TEXT)
                    .replace(
                        "%item%",
                        offer.products
                            .firstOrNull()
                            ?.let { it.displayName.ifBlank { it.name } }
                            .orEmpty(),
                    ).replace("%limit%", perOffer.toString())
            }

            else -> {
                null
            }
        }
    }

    private companion object {
        const val ENABLED = "catalog.limited.daily_cap.is_on"
        const val OVERALL = "catalog.limited.daily_cap.overall"
        const val PER_OFFER = "catalog.limited.daily_cap.per_offer"
        const val OVERALL_TEXT = "catalog.limited.daily_cap.all"
        const val PER_OFFER_TEXT = "catalog.limited.daily_cap.item"
    }
}
