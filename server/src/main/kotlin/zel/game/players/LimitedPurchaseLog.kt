package zel.game.players

/** Written by purchase threads, pruned by housekeeping. */
class LimitedPurchaseLog internal constructor(
    private val now: () -> Int,
) {
    private val purchases = HashMap<Int, MutableList<Int>>(0)

    @Synchronized
    fun add(
        catalogItemId: Int,
        timestamp: Int,
    ) {
        purchases.getOrPut(catalogItemId) { ArrayList(1) }.add(timestamp)
    }

    @Synchronized
    fun total(): Int = purchases.values.sumOf { it.size }

    @Synchronized
    fun total(catalogItemId: Int): Int = purchases[catalogItemId]?.size ?: 0

    @Synchronized
    fun prune() {
        val time = now()
        purchases.values.forEach { times -> times.removeIf { time - it > WINDOW_SECONDS } }
        purchases.values.removeIf { it.isEmpty() }
    }

    companion object {
        const val WINDOW_SECONDS = 86_400
    }
}
