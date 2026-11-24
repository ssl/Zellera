package zel.game.catalog

import zel.game.items.OwnedFurni
import zel.game.players.Player
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException

/** The numbers of one limited edition that are still for sale, in the order its offer hands them out. */
class LimitedEditionStock(
    private val offerId: Int,
    free: Collection<Int>,
    editionSize: Int,
    private val order: LimitedOrder,
    private val services: CatalogServices,
) {
    @Volatile var editionSize: Int = editionSize
        private set

    private val store = LimitedNumberStore(services)
    private val numbers = ArrayDeque(order.arrange(free) { services.random })

    fun reserveNumber(): Int? = synchronized(numbers) { numbers.removeFirstOrNull() }

    fun available(): Int = synchronized(numbers) { numbers.size }

    /** A number whose furni exists stays sold; a returned number is the next one out. */
    fun releaseNumber(number: Int) {
        val free =
            try {
                store.isFreeAgain(offerId, number)
            } catch (e: SQLException) {
                log.error("Returning number {} of limited offer {} failed", number, offerId, e)
                false
            }
        if (free) synchronized(numbers) { if (number !in numbers) numbers.addFirst(number) }
    }

    @Throws(SQLException::class)
    fun markSold(
        connection: Connection,
        player: Player,
        item: OwnedFurni,
    ) = store.recordSale(connection, offerId, item.editionNumber, player.profile.id, item.id)

    fun markSold(
        player: Player,
        item: OwnedFurni,
    ) {
        try {
            services.database.dataSource.connection
                .use { markSold(it, player, item) }
        } catch (e: SQLException) {
            log.error("Recording the sale of number {} of limited offer {} failed", item.editionNumber, offerId, e)
        }
    }

    /** An edition that grew sells its new numbers after the ones already waiting; counting down, before them. */
    fun growTo(size: Int) {
        synchronized(numbers) {
            if (size <= editionSize) return
            val added = order.arrange((editionSize + 1..size).toList()) { services.random }
            if (order == LimitedOrder.DESCENDING) numbers.addAll(0, added) else numbers.addAll(added)
            editionSize = size
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(LimitedEditionStock::class.java)
    }
}
