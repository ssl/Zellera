package zel.game.catalog.marketplace

import zel.db.insertForId
import zel.db.write
import java.sql.Connection
import java.sql.SQLException

/** A listed furni has no owner; every listing change names its starting state, so two moves never both succeed. */
internal object MarketplaceStore {
    const val DELETE_OFFER = "DELETE FROM marketplace_listings WHERE id = ? AND player_id = ? AND state IN (1, 3)"
    const val HAND_OVER = "UPDATE furni SET player_id = ? WHERE id = ? AND player_id IS NULL"
    private const val ESCROW =
        "UPDATE furni SET player_id = NULL WHERE id = ? AND player_id = ? AND room_id IS NULL" +
            " AND NOT EXISTS (SELECT 1 FROM room_playlist_discs WHERE furni_id = furni.id)"
    private const val OPEN =
        "INSERT INTO marketplace_listings (furni_id, player_id, price, listed_at, state) VALUES (?, ?, ?, ?, ?)"
    const val SELL =
        "UPDATE marketplace_listings SET state = 2, sold_at = ? WHERE id = ? AND state = 1 AND listed_at > ?"
    const val DETACH = "UPDATE marketplace_listings SET player_id = NULL WHERE id = ? AND player_id = ? AND state = 2"
    const val CLOSE = "UPDATE marketplace_listings SET state = 3 WHERE id = ? AND state = 1"

    /** False when the furni is not in the inventory (any more) or is in a playlist. */
    @Throws(SQLException::class)
    fun escrow(
        connection: Connection,
        furniId: Int,
        sellerId: Int,
    ): Boolean =
        connection.write(ESCROW) {
            int(furniId)
            int(sellerId)
        } > 0

    @Throws(SQLException::class)
    fun open(
        connection: Connection,
        furniId: Int,
        sellerId: Int,
        price: Int,
        listedAt: Int,
    ): Int? =
        connection.insertForId(OPEN) {
            int(furniId)
            int(sellerId)
            int(price)
            int(listedAt)
            int(ListingState.OPEN.code)
        }

    @Throws(SQLException::class)
    fun handOver(
        connection: Connection,
        furniId: Int,
        playerId: Int,
    ): Boolean =
        connection.write(HAND_OVER) {
            int(playerId)
            int(furniId)
        } > 0

    @Throws(SQLException::class)
    fun markSold(
        connection: Connection,
        offerId: Int,
        soldAt: Int,
    ): Boolean =
        connection.write(SELL) {
            int(soldAt)
            int(offerId)
            int(MarketplacePrices.openSince(soldAt))
        } > 0

    @Throws(SQLException::class)
    fun deleteOffer(
        connection: Connection,
        offerId: Int,
        sellerId: Int,
    ): Boolean =
        connection.write(DELETE_OFFER) {
            int(offerId)
            int(sellerId)
        } > 0

    @Throws(SQLException::class)
    fun detach(
        connection: Connection,
        offerId: Int,
        sellerId: Int,
    ): Boolean =
        connection.write(DETACH) {
            int(offerId)
            int(sellerId)
        } > 0

    /** A sale that got there first stays sold. */
    @Throws(SQLException::class)
    fun close(
        connection: Connection,
        offerId: Int,
    ): Boolean = connection.write(CLOSE) { int(offerId) } > 0
}
