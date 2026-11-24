package zel.game.items.mysterybox

import kotlin.random.Random

/** A ticket's chance is its weight over the sum; weight 0 never comes out. */
class WeightedDraw(
    tickets: List<Pair<Int, Int>>,
) {
    private val weighted = tickets.filter { (_, weight) -> weight > 0 }
    private val ids = weighted.map { (id, _) -> id }
    private val upperBounds = weighted.map { (_, weight) -> weight }.runningReduce(Int::plus)
    private val span = upperBounds.lastOrNull() ?: 0

    fun draw(dice: Random): Int? {
        if (span == 0) return null
        val point = dice.nextInt(span)
        return ids[upperBounds.indexOfFirst { point < it }]
    }
}
