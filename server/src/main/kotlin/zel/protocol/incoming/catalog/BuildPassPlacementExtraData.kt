package zel.protocol.incoming.catalog

import zel.game.catalog.CatalogOffer
import zel.game.items.Item
import zel.game.items.ItemInteraction
import zel.game.items.RecolorablePalettes
import java.util.Locale
import java.util.OptionalInt

object BuildPassPlacementExtraData {
    fun of(
        offer: CatalogOffer,
        baseItem: Item,
        sent: String?,
    ): String {
        val offered = offer.productExtraParam(baseItem)
        val fresh = offered.ifEmpty { offer.presetState }
        val recolorable = baseItem.interaction.name == ItemInteraction.RECOLORABLE
        if (recolorable) {
            val colour = sentColour(sent)
            if (colour.isPresent && RecolorablePalettes.get().allows(baseItem.name, colour.asInt)) {
                return fresh.ifEmpty { "0" } + ";" + "%06X".format(Locale.ROOT, colour.asInt)
            }
        }
        return fresh
    }

    fun sentColour(sent: String?): OptionalInt {
        val parts = sent?.split(",")
        if (parts == null || parts.size != COLOUR_PARTS) return OptionalInt.empty()
        var rgb = 0
        for (part in parts) {
            val value = part.toIntOrNull()
            if (value == null || value !in 0..MAX_CHANNEL) return OptionalInt.empty()
            rgb = (rgb shl BITS_PER_CHANNEL) or value
        }
        return OptionalInt.of(rgb)
    }

    private const val COLOUR_PARTS = 3
    private const val MAX_CHANNEL = 255
    private const val BITS_PER_CHANNEL = 8
}
