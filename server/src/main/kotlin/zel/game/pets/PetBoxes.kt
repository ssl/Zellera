package zel.game.pets

import java.util.concurrent.atomic.AtomicReference

/** A furni that hatches a pet of [type]; with a [palette] it always hatches that one, else a random palette. */
class PetBox(
    val furniName: String,
    val type: Int,
    val palette: Int?,
)

/** The boxes the content defines, by lowercase furni name. */
object PetBoxes {
    const val NEST_ROTATION = 4

    private val boxes = AtomicReference<Map<String, PetBox>>(emptyMap())

    private val gnomeBoxes = AtomicReference<Set<String>>(emptySet())

    fun install(
        all: List<PetBox>,
        behaviourOf: (Int) -> PetBehaviour = { PetBehaviour.PLAIN },
    ) {
        boxes.set(all.associateBy { it.furniName.lowercase() })
        val gnomes = all.filter { behaviourOf(it.type) == PetBehaviour.GNOME }
        gnomeBoxes.set(gnomes.map { it.furniName.lowercase() }.toSet())
    }

    fun isGnome(furniName: String): Boolean = furniName.lowercase() in gnomeBoxes.get()

    fun of(furniName: String): PetBox? = boxes.get()[furniName.lowercase()]

    fun isBox(furniName: String): Boolean = furniName.lowercase() in boxes.get()

    fun canHatch(
        furniName: String,
        catalog: PetCatalog,
    ): Boolean {
        val box = of(furniName) ?: return false
        return when {
            catalog.behaviourOf(box.type) in FIGURE_BUILT -> true
            box.palette != null -> catalog.kind(box.type) != null
            else -> catalog.kind(box.type) != null && catalog.palettesOf(box.type) != null
        }
    }

    private val FIGURE_BUILT = setOf(PetBehaviour.GNOME, PetBehaviour.LEPRECHAUN)
}
