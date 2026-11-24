package zel.game.icons

@Suppress("MagicNumber") // wire and stored state codes
enum class IconState(
    val code: Int,
) {
    PURCHASABLE(0),

    CLAIMABLE(1),
    OWNED(2),

    FAVORITE(3),

    /** Not to be had right now: the reward of an unfinished set, or an icon switched off. */
    UNAVAILABLE(4),

    LOCKED(5),
    ;

    val owned: Boolean get() = this == OWNED || this == FAVORITE

    /** Counts towards its set: owned, or earned and waiting to be claimed. */
    val countsForSet: Boolean get() = owned || this == CLAIMABLE

    companion object {
        /** A stored code; one no state writes reads as [UNAVAILABLE]. */
        fun fromCode(code: Int): IconState = entries.firstOrNull { it.code == code } ?: UNAVAILABLE
    }
}
