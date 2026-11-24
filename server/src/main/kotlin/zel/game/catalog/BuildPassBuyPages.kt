package zel.game.catalog

import zel.game.catalog.layouts.PageForm
import zel.game.items.ProductKind
import zel.game.players.CLUB_MONTH_DAYS
import zel.protocol.generated.PacketWriter

object BuildPassBuyPages {
    private const val PAGE_FIELDS = 3

    fun isBuyPage(page: CatalogPage?): Boolean =
        page != null && page.form is PageForm.BuildPass && page.info.shop != CatalogMode.BUILDER

    fun window(page: CatalogPage?): MembershipOffer.Window =
        (page?.form as? PageForm.BuildPass)?.window ?: MembershipOffer.Window.CLUB

    fun writePage(
        body: PacketWriter,
        page: CatalogPage,
        layoutCode: String,
    ) {
        body.string(layoutCode)
        if (isBuyPage(page)) {
            body.int(0)
            body.int(1)
            body.string(page.info.wording.one)
        } else {
            body.int(PAGE_FIELDS)
            with(page.info.images) { listOf(header, teaser, special) }.forEach { body.string(it) }
            body.int(PAGE_FIELDS)
            with(page.info.wording) { listOf(one, details, teaser) }.forEach { body.string(it) }
        }
    }

    internal fun serializeOffer(
        body: PacketWriter,
        item: CatalogOffer,
        clubOffer: MembershipOffer?,
        form: OfferWire.Form,
    ) {
        OfferWire.writeHead(body, item.id, item.name, item.price, clubOffer != null && clubOffer.giftable, form)

        val products = item.products
        body.int(products.size)
        products.forEach { product ->
            body.string(product.type.code.lowercase())
            if (product.type == ProductKind.BADGE) {
                body.string(product.name)
            } else {
                body.int(product.spriteId)
                body.string("")
                body.int(item.quantityOf(product.id))
                body.boolean(false) // limited
            }
        }

        OfferWire.writeTail(body, 0, false, item.presetState, form)
    }

    fun monthFurniBonus(
        days: Int,
        bonusPerMonth: Int,
    ): Int {
        if (days <= 0 || bonusPerMonth <= 0) return 0
        val bonus = (days / CLUB_MONTH_DAYS).toLong() * bonusPerMonth
        return minOf(Int.MAX_VALUE.toLong(), bonus).toInt()
    }
}
