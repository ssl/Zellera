package zel.game.catalog.marketplace

import zel.game.items.Item
import zel.game.items.OwnedFurni
import zel.game.items.ProductKind
import java.sql.ResultSet

/** On a search hit, [averagePrice] is what the furni sold for lately (0: no sale), [listingCount] its open offers. */
class MarketplaceListing(
    val offerId: Int,
    val furniId: Int,
    val sellerId: Int,
    val price: Int,
    val listedAt: Int,
    state: ListingState,
    soldAt: Int,
    private val definition: Item?,
    val editionNumber: Int,
    val editionSize: Int,
    val averagePrice: Int = price,
    val listingCount: Int = 1,
) {
    @Volatile var state: ListingState = state

    @Volatile var soldAt: Int = soldAt

    val type: Int
        get() =
            when {
                editionNumber > 0 || editionSize > 0 -> LIMITED
                definition?.type == ProductKind.WALL_FURNI -> WALL
                else -> FLOOR
            }

    val spriteId: Int get() = definition?.spriteId ?: 0

    override fun toString(): String = "MarketplaceListing($offerId furni $furniId $price ${state.name})"

    companion object {
        private const val FLOOR = 1
        private const val WALL = 2
        private const val LIMITED = 3

        fun opened(
            offerId: Int,
            furni: OwnedFurni,
            sellerId: Int,
            price: Int,
            listedAt: Int,
        ): MarketplaceListing =
            MarketplaceListing(
                offerId,
                furni.id,
                sellerId,
                price,
                listedAt,
                ListingState.OPEN,
                0,
                furni.definition,
                furni.editionNumber,
                furni.editionSize,
            )

        fun ownRow(
            row: ResultSet,
            definition: (Int) -> Item?,
        ): MarketplaceListing = read(row, definition, row.getInt("price"), 1)

        fun searchRow(
            row: ResultSet,
            definition: (Int) -> Item?,
        ): MarketplaceListing = read(row, definition, row.getInt("average_price"), row.getInt("listing_count"))

        private fun read(
            row: ResultSet,
            definition: (Int) -> Item?,
            averagePrice: Int,
            listingCount: Int,
        ): MarketplaceListing =
            MarketplaceListing(
                offerId = row.getInt("id"),
                furniId = row.getInt("furni_id"),
                sellerId = row.getInt("player_id"),
                price = row.getInt("price"),
                listedAt = row.getInt("listed_at"),
                state = ListingState.fromCode(row.getInt("state")),
                soldAt = row.getInt("sold_at"),
                definition = definition(row.getInt("definition_id")),
                editionNumber = row.getInt("limited_number"),
                editionSize = row.getInt("limited_total"),
                averagePrice = averagePrice,
                listingCount = listingCount,
            )
    }
}
