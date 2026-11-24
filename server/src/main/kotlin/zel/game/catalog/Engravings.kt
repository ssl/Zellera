package zel.game.catalog

import zel.game.items.Item
import zel.game.items.ItemInteraction
import zel.game.items.MusicDisc
import zel.game.players.Player
import zel.util.GameZone
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A plaque is signer, day and inscription, tab-separated. */
class Engravings(
    private val services: CatalogServices,
) {
    fun write(
        player: Player,
        definition: Item,
        text: String,
    ): String =
        when (definition.interaction.name) {
            ItemInteraction.BADGE_DISPLAY -> plaque(player, shownBadge(player, text))
            ItemInteraction.TROPHY -> plaque(player, untabbed(services.wordFilter.filter(inscription(text), player)))
            else -> text
        }

    /** Null for a disc without a song. */
    fun purchasedState(
        player: Player,
        offer: CatalogOffer,
        product: Item,
        text: String,
    ): String? =
        when {
            CatalogPurchaseRules.isWrittenByBuyer(product) -> write(player, product, text)
            product.interaction.name == ItemInteraction.MUSIC_DISC -> discLabel(offer.presetState)
            else -> offer.productExtraParam(product)
        }

    private fun discLabel(songCode: String): String? =
        services.items.jukeboxTrack(songCode)?.let { MusicDisc.state(it.id) }

    private fun plaque(
        player: Player,
        inscription: String,
    ): String = "${player.profile.name}\t${PLAQUE_DAY.format(today())}\t$inscription"

    private fun shownBadge(
        player: Player,
        code: String,
    ): String = if (player.inventory.badges.owns(code)) code else PLACEHOLDER_BADGE

    private fun inscription(text: String): String =
        text.take(services.config.getInt(TROPHY_LENGTH_KEY, DEFAULT_TROPHY_LENGTH))

    private fun untabbed(text: String): String = text.replace("\t", "")

    /** The game's local day, which the buyer reads on the plaque. */
    private fun today(): LocalDate =
        LocalDate.ofInstant(Instant.ofEpochSecond(services.now().toLong()), GameZone.current)

    private companion object {
        private val PLAQUE_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private const val PLACEHOLDER_BADGE = "UMAD"
        private const val TROPHY_LENGTH_KEY = "catalog.engraving.max.characters"
        private const val DEFAULT_TROPHY_LENGTH = 300
    }
}
