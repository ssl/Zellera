package zel.game.catalog

import zel.game.catalog.MembershipOffer.Window

class CatalogClubOffers(
    private val contents: CatalogData,
) {
    /** A deal is never listed; it has its own request. */
    fun listed(window: Window = Window.CLUB): List<MembershipOffer> =
        contents.memberships.values
            .filter { !it.isDeal && it.soldIn(window) }
            .sortedBy { it.id }

    fun forSource(source: Int): List<MembershipOffer> {
        val club = listed()
        if (source != MembershipOffer.SOURCE_VIP_GIFT) return club.filterNot { it.isGift }
        return club.filter { it.isGift }.ifEmpty { club.filter { it.giftable } }
    }

    fun forPage(
        page: CatalogPage,
        itemId: Int,
    ): MembershipOffer? =
        if (BuildPassBuyPages.isBuyPage(page)) {
            page.offer(itemId)?.let { named(it.name, BuildPassBuyPages.window(page)) }
        } else {
            contents.memberships[itemId]?.takeIf { !it.isDeal && it.soldIn(Window.CLUB) }
        }

    private fun named(
        name: String,
        window: Window,
    ): MembershipOffer? {
        if (name.isEmpty()) return null
        return listed(window).firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
