package zel.game.catalog

import zel.game.items.Item
import zel.game.items.ProductKind
import zel.protocol.BodyPart
import zel.protocol.generated.PacketWriter
import java.sql.ResultSet

/** The product spec is `id[:count]` per product, bundles separated by `;`. */
class CatalogOffer(
    row: ResultSet,
    private val services: CatalogServices,
) : BodyPart {
    private class Product(
        val definitionId: Int,
        val count: Int,
    )

    val id: Int = row.getInt("id")
    val pageId: Int = row.getInt("page_id")
    val name: String = row.getString("name").orEmpty()
    private val productSpec: String = row.getString("item_ids").orEmpty()
    val price: OfferPrice =
        OfferPrice.stored(row.getInt("price_credits"), row.getInt("price_currency"), row.getInt("currency"))
    val quantity: Int = row.getInt("amount")
    val editionSize: Int = row.getInt("limited_total")
    private val storedEditionSold: Int = row.getInt("limited_sold")
    val editionOrder: LimitedOrder = LimitedOrder.of(row.getString("limited_order"))
    val presetState: String = row.getString("preset_state").orEmpty()
    val clubOnly: Boolean = row.getBoolean("club_only")
    val hasOffer: Boolean = row.getBoolean("has_offer")
    val clientOfferId: Int = row.getInt("client_offer_id")
    val position: Int = row.getInt("sort_order")

    @Volatile private var resolvedProducts: Set<Item>? = null

    val isLimited: Boolean get() = editionSize > 0

    val editionSold: Int
        get() =
            services.game.catalog
                ?.limited
                ?.get(this)
                ?.let { editionSize - it.available() } ?: storedEditionSold

    val searchOfferId: Int
        get() =
            when {
                clientOfferId > 0 -> clientOfferId
                hasOffer -> id
                else -> NOT_OFFERED
            }

    val bundle: Map<Int, Int>
        get() = if (isBundle) parts().associate { it.definitionId to it.count } else emptyMap()

    val products: Set<Item>
        get() =
            resolvedProducts ?: parts()
                .mapNotNull { services.game.items?.getItem(it.definitionId) }
                .toSet()
                .also { if (it.isNotEmpty()) resolvedProducts = it }

    val giftable: Boolean get() = products.isNotEmpty() && products.all { it.canGift }

    private val isBundle: Boolean get() = BUNDLE_SEPARATOR in productSpec

    fun quantityOf(definitionId: Int): Int = bundle[definitionId] ?: quantity

    fun productExtraParam(item: Item): String =
        SINGLE_PATTERN.matchEntire(name)?.groupValues?.get(1)
            ?: when {
                item.type == ProductKind.BOT -> botFigure()
                item.name.equals(POSTER, ignoreCase = true) || name.startsWith(SONG_PREFIX) -> presetState
                presetState.trim().let { it.startsWith("{") && it.endsWith("}") } -> presetState
                else -> ""
            }

    private fun botFigure(): String =
        presetState
            .split(PRESET_SEPARATOR)
            .firstOrNull { it.startsWith(FIGURE_KEY) }
            ?.removePrefix(FIGURE_KEY)
            ?: presetState

    override fun writeTo(body: PacketWriter) = write(body, OfferWire.Form.PAGE)

    fun writePurchased(body: PacketWriter) = write(body, OfferWire.Form.PURCHASE)

    private fun write(
        body: PacketWriter,
        form: OfferWire.Form,
    ) {
        val furni = products
        val chatStyle = if (furni.isEmpty()) services.chatStyles.styleOfOffer(id) else null
        OfferWire.write(body, this, furni, chatStyle, form)
    }

    private fun parts(): List<Product> =
        productSpec
            .split(BUNDLE_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { part ->
                val fields = part.split(FIELD_SEPARATOR)
                val definitionId = fields[0].trim().toIntOrNull() ?: return@mapNotNull null
                val count = fields.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                if (definitionId <= 0 || count <= 0) return@mapNotNull null
                Product(definitionId, count)
            }

    override fun toString(): String = "CatalogOffer($id $name)"

    private companion object {
        const val NOT_OFFERED = -1
        const val BUNDLE_SEPARATOR = ";"
        const val FIELD_SEPARATOR = ":"
        const val POSTER = "poster"
        const val SONG_PREFIX = "SONG "
        const val PRESET_SEPARATOR = ";"
        const val FIGURE_KEY = "figure:"
        val SINGLE_PATTERN = Regex("[a-z]+_single_(.+)")
    }
}
