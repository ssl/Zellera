package zel.game.catalog

import java.util.Random

/** How a limited edition hands out its numbers: a value of the offer, set by the catalogue's content. */
enum class LimitedOrder(
    val code: String,
) {
    RANDOM("random"),
    ASCENDING("ascending"),
    DESCENDING("descending"),
    ;

    fun arrange(
        numbers: Collection<Int>,
        random: () -> Random,
    ): List<Int> =
        when (this) {
            RANDOM -> numbers.shuffled(random())
            ASCENDING -> numbers.sorted()
            DESCENDING -> numbers.sortedDescending()
        }

    companion object {
        fun of(code: String?): LimitedOrder = entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: RANDOM
    }
}
