package zel.game.catalog

import zel.game.pets.PetPalette

/** Order extra data is `name\nrace\ncolour`, the race being one of the type's palettes. */
internal object PetOrders {
    const val PARTS = 3
    private val COLOUR = Regex("[0-9A-Fa-f]{6}")

    fun picksSellablePalette(
        extraData: String,
        palettes: List<PetPalette>?,
    ): Boolean {
        val parts = extraData.split("\n")
        if (parts.size < PARTS) return false
        val race = parts[1].toIntOrNull() ?: return false
        return COLOUR.matches(parts[2]) && palettes.orEmpty().any { it.sellable && it.paletteId == race }
    }
}
