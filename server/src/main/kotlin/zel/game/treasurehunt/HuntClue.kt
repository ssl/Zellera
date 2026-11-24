package zel.game.treasurehunt

/** One thing to find: a furni to click in a room or a step code the client reports. */
sealed interface HuntClue {
    val id: Int

    data class Furni(
        override val id: Int,
        val furniId: Int,
    ) : HuntClue

    data class Step(
        override val id: Int,
        val code: String,
    ) : HuntClue
}
