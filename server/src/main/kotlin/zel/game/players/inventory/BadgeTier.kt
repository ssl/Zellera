// The constants are the tier table itself.
@file:Suppress("MagicNumber")

package zel.game.players.inventory

enum class BadgeTier(
    val code: Int,
    val holdersUpTo: Int,
) {
    UNIQUE(6, 1),
    ULTRA_RARE(5, 3),
    VERY_RARE(4, 5),
    RARE(3, 10),
    UNCOMMON(2, 50),
    COMMON_PLUS(1, 200),
    COMMON(0, Int.MAX_VALUE),
    ;

    companion object {
        val codes: IntRange = COMMON.code..UNIQUE.code

        /** A badge nobody holds counts as common. */
        fun forHolders(holders: Int): BadgeTier =
            if (holders < 1) COMMON else entries.first { holders <= it.holdersUpTo }
    }
}
