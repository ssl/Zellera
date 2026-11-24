package zel.room.unit.pet

import zel.game.pets.HorsePet
import zel.room.unit.pet.PetStore.HorseLook

/**
 * What a care furni does to a horse, read from the furni's own params: `15 <colour>` dyes the coat, `15 2,3 <tint>`
 * dyes the mane, `15 2,3 <style> <tint>` sets the mane's style and `15 4 <part> ...` is a saddle.
 */
object HorseGrooming {
    private const val HORSE = "15"
    private const val SADDLE_SLOT = "4"
    private val MANE_SLOTS = setOf("2,3", "3,2")
    private val GAPS = Regex("\\s+")
    private const val COAT_WORDS = 2
    private const val TINT_WORDS = 3
    private const val STYLE_WORDS = 4
    private const val BREEDS = 4
    private const val PLAIN = "null"

    /** A coat colour's first palette, and whether each of the four breeds has its own palette from there on. */
    private class Coat(
        val palette: Int,
        val perBreed: Boolean = true,
    )

    private val COATS: Map<String, Coat> =
        mapOf(
            "beige" to Coat(2),
            "brown" to Coat(6),
            "dark-brown" to Coat(10),
            "grey" to Coat(14),
            "red" to Coat(18),
            "purple" to Coat(22),
            "light-blue" to Coat(26),
            "gold" to Coat(30),
            "black" to Coat(34),
            "white" to Coat(38),
            "green" to Coat(42),
            "orange" to Coat(46),
            "dark-blue" to Coat(61, perBreed = false),
            "pink" to Coat(65, perBreed = false),
            "yellow" to Coat(69, perBreed = false),
            "cyan" to Coat(73, perBreed = false),
            "lime" to Coat(77),
        )

    fun groomed(
        look: HorseLook,
        params: String,
        definitionId: Int,
    ): HorseLook? {
        val words = params.trim().split(GAPS)
        if (words.first() != HORSE || words.size < COAT_WORDS) return null
        val what = words[1]
        return when {
            words.size == COAT_WORDS -> dyed(look, what)
            what == SADDLE_SLOT -> look.takeUnless { it.saddle }?.copy(saddle = true, saddleDefinitionId = definitionId)
            what !in MANE_SLOTS -> null
            words.size == TINT_WORDS -> first(words[2])?.let { look.copy(hairColor = it) }
            words.size == STYLE_WORDS -> first(words[2])?.let { look.copy(hairStyle = it) }
            else -> null
        }
    }

    /** A dye changes the coat's colour and keeps the horse's breed; the plain dye gives the first coat back. */
    private fun dyed(
        look: HorseLook,
        colour: String,
    ): HorseLook? {
        val name = colour.lowercase()
        val coat = (if (name == PLAIN) COATS.values.first() else COATS[name]) ?: return null
        val breed = HorsePet.breedOfCoat(look.palette).coerceIn(1, BREEDS)
        return look.copy(palette = if (coat.perBreed) coat.palette + breed - 1 else coat.palette)
    }

    private fun first(numbers: String): Int? = numbers.substringBefore(',').toIntOrNull()
}
