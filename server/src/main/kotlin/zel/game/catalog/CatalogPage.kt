package zel.game.catalog

import zel.game.catalog.layouts.PageForm
import java.util.Collections

class CatalogPage(
    val info: PageInfo,
    val form: PageForm,
    private val now: () -> Int = { 0 },
) : Comparable<CatalogPage> {
    val id: Int get() = info.id

    /** The loader sets the parent once, before the page is in a tree readers see. */
    private var parent: CatalogPage? = null

    /** Offers by row id; iterate under the map's own lock. */
    val offers: MutableMap<Int, CatalogOffer> = Collections.synchronizedMap(LinkedHashMap())

    val offerIds: MutableList<Int> = Collections.synchronizedList(ArrayList())

    val includedPageIds: MutableList<Int> = ArrayList()

    val minRank: Int
        get() = generateSequence(this) { it.parent }.take(MAX_DEPTH).maxOf { it.info.minRank }

    val hasEnded: Boolean get() = info.endsAt?.let { it <= now() } == true

    val secondsToEnd: Int? get() = info.endsAt?.let { it - now() }?.takeIf { it > 0 }

    /** A folder groups pages in the index and has no page of its own to open. */
    val isFolder: Boolean get() = form === PageForm.Folder

    val isOpen: Boolean get() = info.visible && info.enabled && !isFolder

    /** A disabled page does not exist for players, and neither does anything under it. */
    private val exists: Boolean
        get() = generateSequence(this) { it.parent }.take(MAX_DEPTH).all { it.info.enabled }

    val isClubGiftPage: Boolean get() = form.role == PageForm.Role.CLUB_GIFT

    fun adopt(child: CatalogPage) {
        child.parent = this
    }

    fun add(offer: CatalogOffer) {
        offers[offer.id] = offer
    }

    fun offer(id: Int): CatalogOffer? = offers[id]

    /** In the index, shown in the menu or not: an invisible page is still sent, so a link to its id opens it. */
    fun listedFor(
        rank: Int,
        isClubMember: Boolean,
    ): Boolean = exists && !hasEnded && minRank <= rank && (isClubMember || !info.clubOnly)

    fun opensFor(rank: Int): Boolean = exists && !isFolder && !hasEnded && minRank <= rank

    /** Every purchase path asks this; club gifts are chosen, never bought. */
    fun sellsTo(
        rank: Int,
        isClubMember: Boolean,
    ): Boolean = opensFor(rank) && (isClubMember || !info.clubOnly) && !isClubGiftPage

    override fun compareTo(other: CatalogPage): Int = compareValuesBy(this, other, { it.info.position }, { it.id })

    companion object {
        private const val MAX_DEPTH = 20

        fun root(): CatalogPage = CatalogPage(PageInfo.ROOT, PageForm.Root)
    }
}
