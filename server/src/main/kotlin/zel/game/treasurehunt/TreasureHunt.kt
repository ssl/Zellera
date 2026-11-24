package zel.game.treasurehunt

/** A hunt over its open [clues]; [entry] gates it by account level. */
data class TreasureHunt(
    val id: Int,
    val code: String,
    val entry: Entry,
    val prizes: List<Prize>,
    val clues: List<HuntClue>,
) {
    val steps: Int get() = clues.size

    data class Entry(
        val level: Int,
        val clubLevel: Int,
    ) {
        fun admits(
            accountLevel: Int,
            clubMember: Boolean,
        ): Boolean = accountLevel >= if (clubMember) clubLevel else level
    }

    sealed interface Prize {
        data class Badge(
            val code: String,
        ) : Prize

        data class Points(
            val type: Int,
            val amount: Int,
        ) : Prize

        data class Furni(
            val definitionId: Int,
            val copies: Int,
        ) : Prize
    }
}
