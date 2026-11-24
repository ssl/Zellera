package zel.protocol.incoming.catalog

import zel.db.Sql
import zel.db.SqlFailure
import zel.game.catalog.CatalogServices
import zel.game.catalog.InventoryRoom
import zel.game.players.inventory.Inventory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** 0 when nobody has the name. */
internal class GiftReceivers(
    private val services: CatalogServices,
) {
    private val queries = Sql(services.database.dataSource)

    fun idOf(name: String): Int {
        services.players.find(name)?.let { return it.profile.id }
        return try {
            queries.select(ID_BY_NAME, name).first { it.getInt(1) } ?: NOBODY
        } catch (e: SqlFailure) {
            log.error("Looking up a gift receiver failed", e)
            NOBODY
        }
    }

    /** A gift is one more furni in the receiver's inventory, online or not. */
    fun hasRoomForGift(receiverId: Int): Boolean {
        val held =
            services.players
                .find(receiverId)
                ?.inventory
                ?.furni
                ?.count
                ?: try {
                    queries.select(INVENTORY_SIZE, receiverId).first { it.getInt(1) }
                } catch (e: SqlFailure) {
                    log.error("Counting the inventory of gift receiver {} failed", receiverId, e)
                    null
                }
        return held != null && InventoryRoom.fits(held, 1, Inventory.maximumItems)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GiftReceivers::class.java)
        const val NOBODY = 0
        const val ID_BY_NAME = "SELECT id FROM players WHERE name = ?"
        const val INVENTORY_SIZE = "SELECT COUNT(*) FROM furni WHERE player_id = ? AND room_id IS NULL"
    }
}
