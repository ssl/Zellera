package zel.game.catalog

import zel.game.items.Item
import zel.game.items.ProductKind
import zel.protocol.generated.PacketWriter

internal object OfferWire {
    private const val CHAT_STYLE_PRODUCT = "chat_style"

    private const val CLUB_LEVEL_HC = 2

    /** Where the offer goes: a page adds a spare int after the price and the preview image after the bulk flag. */
    enum class Form {
        PAGE,
        PURCHASE,
    }

    fun writeHead(
        body: PacketWriter,
        id: Int,
        name: String,
        price: OfferPrice,
        giftable: Boolean,
        form: Form,
    ) {
        body.int(id)
        body.string(name)
        body.boolean(false)
        body.int(price.credits)
        body.int(price.points)
        body.int(price.pointsType)
        if (form == Form.PAGE) body.int(0)
        body.boolean(giftable)
    }

    fun writeTail(
        body: PacketWriter,
        clubLevel: Int,
        bulk: Boolean,
        previewImage: String,
        form: Form,
    ) {
        body.int(clubLevel)
        body.boolean(bulk)
        if (form == Form.PAGE) {
            body.boolean(false)
            body.string(previewImage)
        }
    }

    fun write(
        body: PacketWriter,
        offer: CatalogOffer,
        products: Set<Item>,
        chatStyle: Int?,
        form: Form,
    ) {
        writeHead(body, offer.id, offer.name, offer.price, offer.giftable, form)
        body.int(products.size + if (chatStyle != null) 1 else 0)
        products.forEach { writeProduct(body, offer, it) }
        chatStyle?.let { writeChatStyle(body, it) }
        writeTail(
            body,
            if (offer.clubOnly) CLUB_LEVEL_HC else 0,
            CatalogPurchaseRules.hasBulkDiscount(offer),
            OfferPreviewImages.of(offer.name),
            form,
        )
    }

    private fun writeProduct(
        body: PacketWriter,
        offer: CatalogOffer,
        item: Item,
    ) {
        body.string(item.type.code.lowercase())
        if (item.type == ProductKind.BADGE) {
            body.string(item.name)
            return
        }
        body.int(item.spriteId)
        body.string(OfferExtraParams.shownOnPage(item, offer.productExtraParam(item)))
        body.int(offer.quantityOf(item.id))
        body.boolean(offer.isLimited)
        if (offer.isLimited) {
            body.int(offer.editionSize)
            body.int(offer.editionSize - offer.editionSold)
        }
    }

    private fun writeChatStyle(
        body: PacketWriter,
        style: Int,
    ) {
        body.string(CHAT_STYLE_PRODUCT)
        body.int(0)
        body.string(style.toString())
        body.int(1)
        body.boolean(false)
    }
}
