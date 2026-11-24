package zel.game.catalog

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.content.ClientStrings
import zel.db.Params
import zel.db.orFallback
import zel.game.catalog.layouts.PageLayoutRow
import java.sql.ResultSet

/** The builder tree loads after the shop's so shared offer ids can be told apart. */
class CatalogLoader(
    private val services: CatalogServices,
    private val data: CatalogData,
    private val layouts: PageLayouts,
    private val limited: LimitedEditions,
    private val recycler: RecyclerCatalog,
) {
    @Synchronized
    fun loadAll() {
        readLayouts()
        readMemberships()
        readClothing()
        readGiftWrapping()
        limited.load()
        readShopTree()
        readBuilderTree()
        readFeaturedSlots()
        readTargetedOffers()
        recycler.load()
    }

    private inline fun forEachRow(
        table: String,
        sql: String,
        noinline bind: Params.() -> Unit = {},
        crossinline read: (ResultSet) -> Unit,
    ) {
        LOGGER.orFallback("catalog: reading $table", Unit) {
            services.database.queries
                .select(sql, bind)
                .each { rows -> read(rows) }
        }
    }

    private fun readLayouts() {
        val rows = mutableListOf<PageLayoutRow>()
        forEachRow("page layouts", LAYOUTS_SQL) { row -> rows += PageLayoutRow.read(row) }
        layouts.install(rows)
        LOGGER.info("catalog: {} page layouts", layouts.size)
    }

    private fun readShopTree() {
        val pages = readPages(SHOP_PAGES_SQL, "shop")
        forEachRow("page includes", PAGE_INCLUDES_SQL) { row ->
            pages[row.getInt("page_id")]?.includedPageIds?.add(row.getInt("included_page_id"))
        }
        CatalogData.swapTree(data.pages, pages)

        data.rowIdBySearchOffer.clear()
        data.clubGifts.clear()
        val index = ShopOfferIndex()
        forEachRow("shop offers", SHOP_OFFERS_SQL) { row -> readShopOffer(row, index) }
        index.letRowIdsAnswer()
        mergeIncludedPages(data.pages)
        LOGGER.info("catalog: {} shop pages", data.pages.size)
    }

    private fun readBuilderTree() {
        CatalogData.swapTree(data.builderPages, readPages(BUILDER_PAGES_SQL, "builder"))

        data.builderRowIdBySearchOffer.clear()
        forEachRow("builder offers", BUILDER_OFFERS_SQL) { row ->
            if (row.getString("item_ids") == NO_ITEMS) return@forEachRow
            val page = data.builderPages[row.getInt("page_id")] ?: return@forEachRow
            val offer = CatalogOffer(row, services)
            page.add(offer)
            if (offer.clientOfferId > 0) {
                page.offerIds.add(offer.clientOfferId)
                data.builderRowIdBySearchOffer[offer.clientOfferId] = offer.id
            }
        }
        val clashing = data.builderRowIdBySearchOffer.keys.count(data.rowIdBySearchOffer::containsKey)
        if (clashing > 0) {
            LOGGER.warn(
                "catalog: {} builder offer ids are shop offer ids too; builder ids start at {}, rebuild the catalog",
                clashing,
                BUILDER_OFFER_IDS_FROM,
            )
        }
        mergeIncludedPages(data.builderPages)
        LOGGER.info("catalog: {} builder pages", data.builderPages.size)
    }

    private fun readPages(
        sql: String,
        tree: String,
    ): Map<Int, CatalogPage> {
        val pages = HashMap<Int, CatalogPage>()
        pages[PageInfo.ROOT_ID] = CatalogPage.root()
        forEachRow("$tree pages", sql) { row -> readPage(row, tree)?.let { pages[it.id] = it } }
        for (page in pages.values) {
            val parentId = page.info.parentId
            val parent = pages[parentId]
            when {
                parent == null && parentId != PageInfo.ROOT.parentId -> {
                    LOGGER.info("catalog: {} page {} ({}) has no parent {}", tree, page.id, page.info.code, parentId)
                }

                parent != null && parent.id != page.id -> {
                    parent.adopt(page)
                }
            }
        }
        return pages
    }

    @Suppress("TooGenericExceptionCaught") // a page whose layout fails to build is left out, not fatal
    private fun readPage(
        row: ResultSet,
        tree: String,
    ): CatalogPage? {
        val layout = row.getString("layout")
        val form = layouts[layout.lowercase()]
        if (form == null) {
            LOGGER.info("catalog: {} page layout {} is not shown", tree, layout)
            return null
        }
        return try {
            CatalogPage(PageInfo.read(row), form, services::now)
        } catch (e: Exception) {
            LOGGER.error("catalog: a {} page with layout {} is unreadable", tree, layout, e)
            null
        }
    }

    private inner class ShopOfferIndex {
        val pageBySearchId = Int2ObjectOpenHashMap<CatalogPage>()
        val offers = ArrayList<CatalogOffer>()

        fun add(
            offer: CatalogOffer,
            page: CatalogPage,
        ) {
            page.add(offer)
            page.offerIds.add(offer.id)
            offers.add(offer)
            val searchId = offer.searchOfferId
            if (searchId == -1) return
            if (searchId != offer.id) page.offerIds.add(searchId)
            // Hidden source pages share offer ids with their visible copies; a search must land on the visible one.
            claim(searchId, offer.id, page)
        }

        fun letRowIdsAnswer() {
            val searchIds = HashSet<Int>(pageBySearchId.keys)
            for (offer in offers) {
                val page = data.pages[offer.pageId] ?: continue
                if (SearchOfferIds.rowIdFree(searchIds, offer.id)) claim(offer.id, offer.id, page)
            }
        }

        private fun claim(
            searchId: Int,
            rowId: Int,
            page: CatalogPage,
        ) {
            if (!SearchOfferIds.pageWins(pageBySearchId[searchId], page)) return
            data.rowIdBySearchOffer[searchId] = rowId
            pageBySearchId[searchId] = page
        }
    }

    private fun readShopOffer(
        row: ResultSet,
        index: ShopOfferIndex,
    ) {
        val problems = OfferRowCheck.of(row).problems()
        if (problems.isNotEmpty()) {
            problems.forEach { LOGGER.error("catalog: offer left out: {}", it) }
            return
        }
        if (row.getString("item_ids") == NO_ITEMS) return
        if (isClubGift(row.getString("name"))) {
            data.clubGifts.add(CatalogOffer(row, services))
            return
        }
        val page = data.pages[row.getInt("page_id")] ?: return
        val offer = CatalogOffer(row, services)
        index.add(offer, page)
        if (offer.isLimited) limited.createOrUpdate(offer)
    }

    private fun isClubGift(name: String?): Boolean {
        val marker = ClientStrings[ClientStrings.CLUB_PRODUCT_PREFIX] ?: return false
        return name?.contains(marker) == true
    }

    private fun mergeIncludedPages(pages: Int2ObjectMap<CatalogPage>) {
        for (page in pages.values.toList()) {
            for (id in page.includedPageIds) {
                pages[id]?.let { page.offers.putAll(it.offers) }
            }
        }
    }

    private fun readFeaturedSlots() {
        val slots: Int2ObjectMap<FeaturedPageSlot> = Int2ObjectOpenHashMap()
        forEachRow("featured slots", "SELECT * FROM catalogue_featured_slots ORDER BY slot ASC") { row ->
            slots.put(
                row.getInt("slot"),
                FeaturedPageSlot(
                    row.getInt("slot"),
                    row.getString("caption"),
                    row.getString("image"),
                    FeaturedLink.of(
                        row.getString("link_kind"),
                        row.getString("page_name"),
                        row.getInt("page_id"),
                        row.getString("product_name"),
                    ),
                    row.getInt("expires_at").takeUnless { row.wasNull() },
                    services,
                ),
            )
        }
        data.swapFeaturedSlots(slots)
    }

    private fun readMemberships() {
        data.memberships.clear()
        forEachRow("club offers", "SELECT * FROM club_offers WHERE enabled = ?", { boolean(true) }) { row ->
            data.memberships[row.getInt("id")] = MembershipOffer.read(row, services::now)
        }
    }

    private fun readTargetedOffers() {
        val offers = HashMap<Int, TargetedOffer>()
        forEachRow("targeted offers", TARGETED_OFFERS_SQL, { int(services.now()) }) { row ->
            offers[row.getInt("id")] = TargetedOffer.fromRow(row, services)
        }
        CatalogData.refill(data.targeted, offers)
    }

    private fun readClothing() {
        val sets = HashMap<Int, ClothingSet>()
        forEachRow("clothing", CLOTHING_SQL) { row -> sets[row.getInt("id")] = ClothingSet.fromRow(row) }
        CatalogData.refill(data.clothingSets, sets)
    }

    private fun readGiftWrapping() {
        val paper = HashMap<Int, Int>()
        val boxes = HashMap<Int, Int>()
        forEachRow("gift wrapping", GIFT_WRAPPING_SQL) { row ->
            when (row.getString("kind")) {
                "wrapper" -> paper[row.getInt("sprite_id")] = row.getInt("definition_id")
                "gift" -> boxes[row.getInt("sprite_id")] = row.getInt("definition_id")
            }
        }
        data.swapGiftWrapping(paper, boxes)
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(CatalogLoader::class.java)

        const val NO_ITEMS = "0"

        /** Builder offer ids start here: one number must mean one offer in both trees. */
        const val BUILDER_OFFER_IDS_FROM = 1_000_000

        /** Builder pages are stored this far above the shop's and read back from 1. */
        const val BUILDER_PAGE_ID_SHIFT = 1_000_000

        const val LAYOUTS_SQL = "SELECT * FROM catalogue_layouts ORDER BY code"
        const val SHOP_PAGES_SQL =
            "SELECT * FROM catalogue_pages WHERE catalogue = 'normal' ORDER BY parent_id, id"
        const val PAGE_INCLUDES_SQL =
            "SELECT page_id, included_page_id FROM catalogue_page_includes ORDER BY page_id, position"
        const val BUILDER_PAGES_SQL =
            "SELECT p.id - $BUILDER_PAGE_ID_SHIFT AS id, p.parent_id - $BUILDER_PAGE_ID_SHIFT AS parent_id," +
                " p.caption, p.code, p.layout, p.icon_image, p.min_rank, p.sort_order, p.visible, p.enabled," +
                " p.club_only, p.catalogue, p.headline, p.teaser, p.special_image, p.text_1," +
                " p.text_2, p.text_details, p.text_teaser, p.room_id, p.ends_at, p.expiry_image" +
                " FROM catalogue_pages p WHERE p.catalogue = 'build_pass' ORDER BY p.parent_id, p.id"

        private const val OFFER_ITEM_IDS =
            "COALESCE((SELECT GROUP_CONCAT(" +
                "IF(i.amount IS NULL, i.definition_id, CONCAT(i.definition_id, ':', i.amount))" +
                " ORDER BY i.position SEPARATOR ';') FROM catalogue_offer_items i WHERE i.offer_id = o.id), '') AS item_ids"
        private const val OFFER_COLUMNS =
            "o.id, o.name, o.price_credits, o.price_currency, o.currency, o.amount, o.limited_total, o.limited_sold," +
                " o.limited_order, o.sort_order, o.client_offer_id, o.preset_state, o.has_offer, o.club_only"
        const val SHOP_OFFERS_SQL =
            "SELECT $OFFER_COLUMNS, o.page_id, $OFFER_ITEM_IDS FROM catalogue_offers o" +
                " INNER JOIN catalogue_pages p ON p.id = o.page_id WHERE p.catalogue = 'normal' ORDER BY o.id"
        const val BUILDER_OFFERS_SQL =
            "SELECT $OFFER_COLUMNS, o.page_id - $BUILDER_PAGE_ID_SHIFT AS page_id, $OFFER_ITEM_IDS" +
                " FROM catalogue_offers o INNER JOIN catalogue_pages p ON p.id = o.page_id" +
                " WHERE p.catalogue = 'build_pass' ORDER BY o.id"

        const val TARGETED_OFFERS_SQL =
            "SELECT t.*, (SELECT GROUP_CONCAT(v.value ORDER BY v.position SEPARATOR ';') FROM target_offer_vars v" +
                " WHERE v.target_offer_id = t.id) AS vars FROM target_offers t WHERE t.ends_at > ?"
        const val GIFT_WRAPPING_SQL =
            "SELECT kind, sprite_id, definition_id FROM gift_wrappers ORDER BY sprite_id DESC"
        const val CLOTHING_SQL =
            "SELECT c.id, c.name, (SELECT GROUP_CONCAT(s.set_id ORDER BY s.position SEPARATOR ',')" +
                " FROM clothing_sets s WHERE s.clothing_id = c.id) AS set_ids FROM clothing c"
    }
}
