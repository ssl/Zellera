package zel.game.catalog

import zel.game.catalog.layouts.PageForm
import zel.game.players.Player

class Catalog internal constructor(
    private val services: CatalogServices,
    load: Boolean,
) {
    constructor(services: CatalogServices) : this(services, true)

    val contents = CatalogData()
    val pageLayouts: PageLayouts = PageLayouts()
    val settings: CatalogSettings get() = services.settings
    val limited = LimitedEditions(services, contents)
    val recycler = RecyclerCatalog(services, contents)
    val engravings = Engravings(services)
    val membershipOffers = CatalogClubOffers(contents)
    val vouchers =
        VoucherService(
            VoucherStore({ services.dataSource }, services::now),
            this,
            VoucherLockout(services::now),
        ) { services.texts.text(VoucherService.LOCKED_TEXT) }

    @Volatile
    private var revision = 0L

    @Volatile
    private var view: CatalogSnapshot? = null
    private val contentsLoader = CatalogLoader(services, contents, pageLayouts, limited, recycler)
    private val purchases = CatalogPurchases(services, limited)

    init {
        if (load) load()
    }

    fun load() {
        contentsLoader.loadAll()
        changed()
    }

    fun dispose() {
        services.raffles.cancelOpen()
    }

    fun snapshot(): CatalogSnapshot = view ?: rebuildView()

    private fun rebuildView(): CatalogSnapshot {
        val fresh = CatalogSnapshot.of(contents, revision)
        synchronized(this) { if (revision == fresh.version) view = fresh }
        return fresh
    }

    val version: Long get() = revision

    @Synchronized
    fun changed() {
        revision++
        view = null
    }

    fun page(id: Int): CatalogPage? = page(id, CatalogMode.NORMAL)

    fun page(
        id: Int,
        shop: CatalogMode,
    ): CatalogPage? = contents.tree(shop).get(id)

    fun page(code: String?): CatalogPage? = snapshot().page(code)

    fun pageWithRole(role: PageForm.Role): CatalogPage? = snapshot().pageWithRole(role)

    fun offer(id: Int): CatalogOffer? = offer(id, CatalogMode.NORMAL)

    fun offer(
        id: Int,
        shop: CatalogMode,
    ): CatalogOffer? = snapshot().offer(shop, id)

    /** The offer behind a furni's Buy button, when [viewer] may open the page that sells it. */
    fun offerForSearchId(
        clientOfferId: Int,
        viewer: Player,
    ): CatalogOffer? {
        val rank = viewer.profile.rank.id
        shopOfferForSearchId(clientOfferId)
            ?.takeIf { page(it.pageId)?.sellsTo(rank, viewer.stats.club.isMember) == true }
            ?.let { return it }
        val builderRowId = contents.builderRowIdBySearchOffer.get(clientOfferId)
        return offer(builderRowId, CatalogMode.BUILDER)
            ?.takeIf { builderRowId > 0 && page(it.pageId, CatalogMode.BUILDER)?.opensFor(rank) == true }
    }

    /** Null when only a Build Pass page has the offer. */
    fun shopOfferForSearchId(clientOfferId: Int): CatalogOffer? {
        if (clientOfferId <= 0) return null
        val rowId = contents.rowIdBySearchOffer.get(clientOfferId)
        return if (rowId > 0) offer(rowId) else null
    }

    fun clubGift(id: Int): CatalogOffer? = snapshot().clubGift(id)

    fun clothingSet(name: String?): ClothingSet? = snapshot().clothing(name)

    fun targetedOffer(id: Int): TargetedOffer? = contents.targeted[id]

    fun runningTargetedOffer(): TargetedOffer? = settings.targetOfferId?.let(::targetedOffer)?.takeIf { it.isRunning }

    /** Children by parent page id (-1: the top level), each list in page order. */
    fun visibleChildren(
        player: Player,
        shop: CatalogMode,
    ): Map<Int, List<CatalogPage>> {
        val staffRank = player.profile.rank.id
        val inClub = player.stats.club.isMember
        val tree = contents.tree(shop)
        val pages = synchronized(tree) { tree.values.toList() }
        return pages
            .filter { shop.lists(it.info.shop) && it.listedFor(staffRank, inClub) }
            .groupBy { it.info.parentId }
            .mapValues { (_, children) -> children.sorted() }
    }

    fun pageEndingFirst(
        rank: Int,
        isClubMember: Boolean,
    ): CatalogPage? {
        val tree = contents.tree(CatalogMode.NORMAL)
        val pages = synchronized(tree) { tree.values.toList() }
        return pages
            .filter { it.secondsToEnd != null && it.info.visible }
            .filter { it.listedFor(rank, isClubMember) && it.opensFor(rank) }
            .minWithOrNull(compareBy<CatalogPage> { it.secondsToEnd }.thenBy { it.id })
    }

    fun calendarDailyOffer(
        rank: Int,
        isClubMember: Boolean,
    ): CatalogOffer? {
        val offer = settings.calendarDailyOfferId?.let(::offer) ?: return null
        return offer.takeIf { page(it.pageId)?.sellsTo(rank, isClubMember) == true }
    }

    fun offersShown(page: CatalogPage): List<CatalogOffer> =
        synchronized(page.offers) { page.offers.values.toList() }.sortedWith(settings.itemOrder())

    fun purchase(
        item: CatalogOffer?,
        player: Player,
        amount: Int,
        extraData: String,
        free: Boolean,
    ): Boolean = purchases.purchase(item, player, amount, extraData, free, null)

    fun purchaseAt(
        item: CatalogOffer?,
        player: Player,
        amount: Int,
        price: OfferPrice,
    ): Boolean = purchases.purchase(item, player, amount, "", false, price)
}
