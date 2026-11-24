package zel.game.players.inventory

import zel.protocol.outgoing.inventory.AddRoomItem.UnseenCategory
import java.util.TreeMap

class UnseenItems(
    private val userId: Int,
    private val store: Store,
) {
    interface Store {
        fun load(userId: Int): Map<Int, Set<Int>>

        fun add(
            userId: Int,
            category: Int,
            ids: Collection<Int>,
        )

        fun clear(
            userId: Int,
            category: Int,
        )

        fun remove(
            userId: Int,
            category: Int,
            ids: Collection<Int>,
        )
    }

    private val idsByCategory = TreeMap<Int, MutableSet<Int>>()

    fun load() {
        val stored = store.load(userId)
        synchronized(idsByCategory) {
            stored.forEach { (category, ids) ->
                if (tracked(category) && ids.isNotEmpty()) idsByCategory.getOrPut(category, ::LinkedHashSet).addAll(ids)
            }
        }
    }

    fun mark(
        category: Int,
        itemIds: Collection<Int?>?,
    ) {
        if (category <= 0 || !tracked(category) || itemIds.isNullOrEmpty()) return
        val fresh =
            synchronized(idsByCategory) {
                val known = idsByCategory.getOrPut(category, ::LinkedHashSet)
                itemIds.filterNotNull().filter { it != 0 && known.add(it) }
            }
        if (fresh.isNotEmpty()) store.add(userId, category, fresh)
    }

    fun markAll(byCategory: Map<Int, List<Int>>) = byCategory.forEach { (category, ids) -> mark(category, ids) }

    fun clear(category: Int): Boolean {
        val dropped = synchronized(idsByCategory) { idsByCategory.remove(category) }
        if (dropped.isNullOrEmpty()) return false
        store.clear(userId, category)
        return true
    }

    fun seen(
        category: Int,
        itemIds: Set<Int>?,
    ): Int {
        if (itemIds.isNullOrEmpty()) return 0
        val seenNow =
            synchronized(idsByCategory) {
                val known = idsByCategory[category] ?: return 0
                itemIds.filter(known::remove).also { if (known.isEmpty()) idsByCategory.remove(category) }
            }
        if (seenNow.isNotEmpty()) store.remove(userId, category, seenNow)
        return seenNow.size
    }

    fun snapshot(): Map<Int, List<Int>> =
        synchronized(idsByCategory) {
            idsByCategory.mapValuesTo(TreeMap()) { (_, ids) -> ids.toList() }
        }

    private fun tracked(category: Int) = category != UnseenCategory.ICON.number

    companion object {
        /** A longer reset list is ignored. */
        const val MOST_IDS_PER_RESET = 1000
    }
}
