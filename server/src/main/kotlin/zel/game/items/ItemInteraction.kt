package zel.game.items

class ItemInteraction(
    raw: String?,
) {
    val name: String = raw?.trim()?.lowercase()?.ifEmpty { DEFAULT } ?: DEFAULT

    val family: Family = Family.entries.find { it.covers(name) } ?: Family.NONE

    val isTeleport: Boolean get() = family == Family.TELEPORT
    val isGroupFurni: Boolean get() = family == Family.GROUP
    val isFurniStorage: Boolean get() = family == Family.FURNI_STORAGE
    val isStorage: Boolean get() = family == Family.FURNI_STORAGE || family == Family.COIN_STORAGE

    fun isAny(vararg names: String): Boolean = name in names

    override fun equals(other: Any?): Boolean = other is ItemInteraction && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    enum class Family(
        private val exact: Set<String> = emptySet(),
        private val prefix: String? = null,
    ) {
        TELEPORT(setOf("transit.door", "transit.tile", "transit.tile.club", "wf_room_linker")),
        GROUP(setOf(GUILD_FURNI, "gate.group")),
        FURNI_STORAGE(prefix = "wf_storage_furni"),
        COIN_STORAGE(prefix = "wf_storage_coins"),
        NONE {
            override fun covers(name: String) = true
        },
        ;

        open fun covers(name: String): Boolean = name in exact || (prefix != null && name.startsWith(prefix))
    }

    companion object {
        const val DEFAULT = "default"
        const val BADGE_DISPLAY = "display.badge"
        const val EXTERNAL_IMAGE = "display.image"
        const val GIFT = "gift.box"
        const val GUILD_FURNI = "group.furni"
        const val MONSTER_PLANT_SEED = "pet.seed"
        const val MUSIC_DISC = "music.disc"
        const val NEST = "pet.nest"
        const val PET_DRINK = "pet.drink"
        const val PET_FOOD = "pet.food"
        const val PET_TOY = "pet.toy"
        const val PET_TRAMPOLINE = "pet.trampoline"
        const val RECYCLER_BOX = "recycler.box"
        const val POST_IT = "note.sticky"
        const val RECOLORABLE = "state.recolor"
        const val TROPHY = "display.trophy"
    }
}
