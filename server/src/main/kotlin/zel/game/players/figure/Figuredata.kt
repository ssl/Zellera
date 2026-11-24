package zel.game.players.figure

data class Figuredata(
    val palettes: Map<Int, Palette> = emptyMap(),
    val setTypes: Map<String, SetType> = emptyMap(),
) {
    val isEmpty: Boolean get() = palettes.isEmpty() || setTypes.isEmpty()

    val typeOrder: List<String> get() = setTypes.keys.toList()

    data class Color(
        val id: Int,
        val index: Int,
        val clubOnly: Boolean,
        val selectable: Boolean,
    )

    data class Palette(
        val id: Int,
        val colors: List<Color>,
    ) {
        private val byId = colors.associateBy(Color::id)

        fun color(id: Int): Color? = byId[id]

        val defaultColor: Color? get() = colors.minByOrNull(Color::index)

        val freeColor: Color?
            get() {
                val sorted = colors.sortedBy(Color::id)
                return sorted.firstOrNull { !it.clubOnly && it.selectable } ?: sorted.firstOrNull()
            }
    }

    data class ClothingSet(
        val id: Int,
        val gender: String,
        val clubOnly: Boolean,
        val colorable: Boolean,
        val selectable: Boolean,
        val sellable: Boolean,
        val colorCount: Int = 0,
    ) {
        fun fits(gender: String): Boolean =
            this.gender.equals(UNISEX, ignoreCase = true) || this.gender.equals(gender, ignoreCase = true)

        fun wearableBy(wearer: Wearer): Boolean =
            selectable && fits(wearer.gender) && (!clubOnly || wearer.club) &&
                (!sellable || wearer.ownsAll || id in wearer.ownedSets)
    }

    data class SetType(
        val type: String,
        val paletteId: Int,
        val requiredFor: Set<Requirement>,
        val sets: List<ClothingSet>,
    ) {
        private val byId = sets.associateBy(ClothingSet::id)
        private val highestFirst = sets.sortedByDescending(ClothingSet::id)

        fun set(id: Int): ClothingSet? = byId[id]

        /** A figure of an unknown gender may never go without a slot. */
        fun optionalFor(wearer: Wearer): Boolean =
            Requirement.genders.any { it.equals(wearer.gender, ignoreCase = true) } &&
                Requirement(wearer.gender.uppercase(), wearer.club) !in requiredFor

        fun fallbackFor(gender: String): ClothingSet? {
            val candidates = highestFirst.filter { it.fits(gender) && !it.sellable && it.selectable }
            return candidates.firstOrNull { !it.clubOnly } ?: candidates.firstOrNull() ?: highestFirst.firstOrNull()
        }
    }

    data class Requirement(
        val gender: String,
        val club: Boolean,
    ) {
        companion object {
            val genders = listOf("M", "F")
        }
    }

    data class Wearer(
        val gender: String,
        val club: Boolean,
        val ownedSets: Collection<Int>,
        val ownsAll: Boolean = false,
    )

    private companion object {
        const val UNISEX = "U"
    }
}
