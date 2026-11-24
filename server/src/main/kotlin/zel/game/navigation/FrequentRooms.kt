package zel.game.navigation

/** The "frequently visited" list: rooms in the iteration order of a Java hash set of their ids. */
object FrequentRooms {
    const val KEPT = 4

    const val KEPT_VISITS = 2

    private const val FIRST_TABLE = 16
    private const val FILL_NUMERATOR = 3
    private const val FILL_DENOMINATOR = 4

    fun kept(often: List<Int>): List<Int> = order(often, emptyList()).take(KEPT)

    fun order(
        kept: List<Int>,
        session: List<Int>,
    ): List<Int> {
        val joined = LinkedHashSet(kept).apply { addAll(session) }
        var table = FIRST_TABLE
        while (joined.size * FILL_DENOMINATOR > table * FILL_NUMERATOR) table *= 2
        val mask = table - 1
        // a stable sort: one bucket keeps the joining order, as a hash set's bucket list (and its resize split) does
        return joined.sortedBy { bucket(it, mask) }
    }

    internal fun bucket(
        id: Int,
        mask: Int,
    ): Int = (id xor (id ushr 16)) and mask
}
