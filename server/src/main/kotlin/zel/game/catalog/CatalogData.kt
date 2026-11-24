package zel.game.catalog

import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMaps
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import zel.game.items.Item
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/** Live collections a reload refills in place; views are copies taken under the reload's lock. */
class CatalogData {
    val pages: Int2ObjectMap<CatalogPage> = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap())
    val builderPages: Int2ObjectMap<CatalogPage> = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap())

    val rowIdBySearchOffer: Int2IntMap = Int2IntMaps.synchronize(Int2IntOpenHashMap())
    val builderRowIdBySearchOffer: Int2IntMap = Int2IntMaps.synchronize(Int2IntOpenHashMap())

    val clubGifts: MutableList<CatalogOffer> = Collections.synchronizedList(ArrayList())
    val memberships: MutableMap<Int, MembershipOffer> = ConcurrentHashMap()
    val targeted: MutableMap<Int, TargetedOffer> = ConcurrentHashMap()
    val clothingSets: MutableMap<Int, ClothingSet> = ConcurrentHashMap()

    /** Guarded by itself. */

    val recyclerPrizes: MutableMap<Int, MutableSet<Item>> = ConcurrentHashMap()

    val wrappers: MutableMap<Int, Int> = ConcurrentHashMap()
    val giftFurni: MutableMap<Int, Int> = ConcurrentHashMap()

    val featuredSlots: Int2ObjectMap<FeaturedPageSlot> = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap())
    val limitedNumbers: MutableMap<Int, LimitedEditionStock> = ConcurrentHashMap()

    private val wrappingLock = Any()

    fun tree(mode: CatalogMode?): Int2ObjectMap<CatalogPage> = if (mode == CatalogMode.BUILDER) builderPages else pages

    fun giftWrapping(): GiftWrapping =
        synchronized(wrappingLock) { GiftWrapping(HashMap(wrappers), HashMap(giftFurni)) }

    fun swapGiftWrapping(
        paper: Map<Int, Int>,
        boxes: Map<Int, Int>,
    ) = synchronized(wrappingLock) {
        refill(wrappers, paper)
        refill(giftFurni, boxes)
    }

    fun recyclerPrizesByLevel(): Map<Int, Set<Item>> = recyclerPrizes.mapValues { (_, items) -> LinkedHashSet(items) }

    fun featuredPages(): List<FeaturedPageSlot> = synchronized(featuredSlots) { ArrayList(featuredSlots.values) }

    fun swapFeaturedSlots(loaded: Int2ObjectMap<FeaturedPageSlot>) =
        synchronized(featuredSlots) {
            featuredSlots.clear()
            featuredSlots.putAll(loaded)
        }

    companion object {
        /** Swaps a page tree in one step: a reader that locks the map waits for the whole new tree. */
        fun swapTree(
            live: Int2ObjectMap<CatalogPage>,
            reloaded: Map<Int, CatalogPage>,
        ) = synchronized(live) {
            live.clear()
            live.putAll(reloaded)
        }

        fun <K, V> refill(
            live: MutableMap<K, V>,
            contents: Map<K, V>,
        ) = synchronized(live) {
            live.clear()
            live.putAll(contents)
        }
    }
}
