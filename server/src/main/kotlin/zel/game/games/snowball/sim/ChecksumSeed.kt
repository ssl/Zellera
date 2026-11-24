package zel.game.games.snowball.sim

object ChecksumSeed {
    private const val FIRST_SHIFT = 13
    private const val SECOND_SHIFT = 17
    private const val THIRD_SHIFT = 5

    fun next(seed: Int): Int =
        (if (seed == 0) -1 else seed)
            .let { it xor (it shl FIRST_SHIFT) }
            .let { it xor (it shr SECOND_SHIFT) }
            .let { it xor (it shl THIRD_SHIFT) }
}
