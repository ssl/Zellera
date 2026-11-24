package zel.room.trade

import zel.game.items.OwnedFurni
import kotlin.math.abs

/** Turns the item ids of one offer request into furni; a request is taken whole or not at all. */
object OfferedFurni {
    fun one(
        itemId: Int,
        lookup: (Int) -> OwnedFurni?,
    ): OwnedFurni? = lookup(itemId)?.takeIf { it.definition.canTrade }

    fun batch(
        itemIds: List<Int>,
        cap: Int,
        lookup: (Int) -> OwnedFurni?,
    ): List<OwnedFurni>? {
        if (itemIds.size !in 1..cap || itemIds.any(::unusable) || itemIds.distinctBy(::abs).size < itemIds.size) {
            return null
        }
        val furni = itemIds.map { one(it, lookup) ?: return null }
        return furni.takeIf { it.toSet().size == it.size }
    }

    private fun unusable(id: Int): Boolean = id == 0 || id == Int.MIN_VALUE
}
