package zel.game.pets

/** How rare a baby's breed is, rarest first: the breeding window lists the tiers in this order. */
enum class BabyTier {
    EPIC,
    RARE,
    UNCOMMON,
    COMMON,
    ;

    /** The tier a draw falls to when this one has no breeds; null below the commonest. */
    val commoner: BabyTier? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun stored(name: String?): BabyTier? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
