package zel.game.pets

/** What a pet type does beyond the plain pet: the content names it per type in `pet_types.behaviour`. */
enum class PetBehaviour {
    PLAIN,
    GNOME,
    LEPRECHAUN,
    COW,
    ;

    companion object {
        fun stored(code: String?): PetBehaviour =
            entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: PLAIN
    }
}
