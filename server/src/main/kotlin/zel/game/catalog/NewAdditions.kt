package zel.game.catalog

object NewAdditions {
    const val PAGE_CODE = "new_additions"

    fun newestOfferId(catalog: Catalog): Int {
        val page = catalog.page(PAGE_CODE)?.takeIf { it.info.visible && it.info.enabled } ?: return 0
        return synchronized(page.offers) { page.offers.keys.maxOrNull() } ?: 0
    }

    fun waiting(
        catalog: Catalog,
        seenOfferId: Int,
    ): Boolean = newestOfferId(catalog) > seenOfferId
}
