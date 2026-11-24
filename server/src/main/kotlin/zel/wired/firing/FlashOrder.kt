package zel.wired.firing

import kotlin.random.Random

/**
 * Where a furni goes inside a firing's ObjectsDataUpdate, in each of its two groups (changed furni, then boxes; see
 * [FlashOut.send]). The order holds for the whole time a room is loaded, whatever order the effects ran in, and a new
 * load draws a new one. Nothing in the room decides it: not the furni id, the stack order, the pick order, the tile
 * or the placement order. A furni placed while the room is loaded gets its place when it first goes out.
 */
class FlashOrder(
    private val random: Random = Random.Default,
) {
    private val ranks = HashMap<Int, Int>()

    fun rank(furniId: Int): Int = ranks.getOrPut(furniId) { random.nextInt() }
}
