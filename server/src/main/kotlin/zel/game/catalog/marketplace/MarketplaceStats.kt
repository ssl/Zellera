package zel.game.catalog.marketplace

import org.slf4j.LoggerFactory
import zel.db.Params
import zel.db.mapRows
import zel.db.orFallback
import zel.game.catalog.CatalogServices
import zel.protocol.generated.PacketWriter
import zel.util.SECONDS_PER_DAY
import java.sql.ResultSet

class MarketplaceStats(
    private val services: CatalogServices,
) {
    private data class SoldDay(
        val daysAgo: Int,
        val averagePrice: Int,
        val unitsSold: Int,
    )

    private data class OpenOffers(
        val count: Int,
        val cheapest: Int,
    )

    fun writeItemStats(
        kind: Int,
        spriteId: Int,
        body: PacketWriter,
    ) {
        val now = services.now()
        val fee = services.settings.marketplaceFee
        val history = soldDays(kind, spriteId, now) ?: return
        val open = openOffers(kind, spriteId, now)
        body.int(fee.buyerPays(recentAverage(kind, spriteId, now)))
        body.int(open.count)
        body.int(HISTORY_DAYS)
        body.int(history.size)
        history.forEach {
            body.int(-it.daysAgo)
            body.int(fee.buyerPays(it.averagePrice))
            body.int(it.unitsSold)
        }
        body.int(kind)
        body.int(spriteId)
        val cheapest = if (open.cheapest > 0) fee.buyerPays(open.cheapest) else 0
        body.int(cheapest)
        body.int(cheapest)
    }

    private fun soldDays(
        kind: Int,
        spriteId: Int,
        now: Int,
    ): List<SoldDay>? =
        read("sales per day of sprite $spriteId", null, dailySalesSql(kind), {
            int(utcDay(now))
            int(spriteId)
            int(firstSecondOfDaysAgo(now, HISTORY_DAYS - 1))
        }) { rows ->
            rows.mapRows { SoldDay(it.getInt("days_ago"), it.getInt("average_price"), it.getInt("units_sold")) }
        }

    private fun openOffers(
        kind: Int,
        spriteId: Int,
        now: Int,
    ): OpenOffers =
        read("open offers of sprite $spriteId", OpenOffers(0, 0), openListingsSql(kind), {
            int(spriteId)
            int(MarketplacePrices.openSince(now))
        }) { rows ->
            if (rows.next()) OpenOffers(rows.getInt("open_offers"), rows.getInt("cheapest")) else OpenOffers(0, 0)
        }

    private fun recentAverage(
        kind: Int,
        spriteId: Int,
        now: Int,
    ): Int =
        read("average price of sprite $spriteId", 0, recentAverageSql(kind), {
            int(spriteId)
            int(firstSecondOfDaysAgo(now, AVERAGE_DAYS - 1))
        }) { rows -> if (rows.next()) rows.getInt("average_price") else 0 }

    private fun <T> read(
        what: String,
        fallback: T,
        sql: String,
        bind: Params.() -> Unit,
        rows: (ResultSet) -> T,
    ): T =
        log.orFallback("marketplace stats: $what", fallback) {
            services.database.queries
                .select(sql, bind)
                .result(rows)
        }

    companion object {
        private val log = LoggerFactory.getLogger(MarketplaceStats::class.java)

        private const val OPEN = 1
        private const val SOLD = 2

        /** The graph covers today and the 29 days before, the average today and the 6 before; halves round up. */
        const val HISTORY_DAYS = 30
        const val AVERAGE_DAYS = MarketplacePrices.AVERAGE_DAYS

        const val KIND_FLOOR = 1
        const val KIND_WALL = 2
        const val KIND_LIMITED = 3

        /** A sale stored without a sale time is dated by its listing time. */
        const val SOLD_AT = "COALESCE(l.sold_at, l.listed_at)"

        /** The sprite is the first value. */
        private const val LISTINGS_OF_SPRITE =
            " FROM marketplace_listings l" +
                " JOIN furni f ON f.id = l.furni_id" +
                " JOIN furni_definitions d ON d.id = f.definition_id" +
                " WHERE d.sprite_id = ?"

        private const val NOT_LIMITED = " AND f.limited_number = 0 AND f.limited_total = 0"

        fun kindFilter(kind: Int): String =
            when (kind) {
                KIND_LIMITED -> " AND (f.limited_number <> 0 OR f.limited_total <> 0)"
                KIND_WALL -> " AND d.kind = 'i'$NOT_LIMITED"
                else -> " AND d.kind = 's'$NOT_LIMITED"
            }

        /** Values: today's UTC day number, the sprite, the first second of the graph's oldest day. */
        fun dailySalesSql(kind: Int): String =
            "SELECT ? - FLOOR($SOLD_AT / $SECONDS_PER_DAY) AS days_ago," +
                " ROUND(AVG(l.price)) AS average_price, COUNT(*) AS units_sold" +
                LISTINGS_OF_SPRITE + kindFilter(kind) +
                " AND l.state = $SOLD AND $SOLD_AT >= ?" +
                " GROUP BY days_ago ORDER BY days_ago DESC"

        /** Values: the sprite, the first second of the oldest day the average counts. */
        fun recentAverageSql(kind: Int): String =
            "SELECT ROUND(AVG(l.price)) AS average_price" + LISTINGS_OF_SPRITE + kindFilter(kind) +
                " AND l.state = $SOLD AND $SOLD_AT >= ?"

        /** Values: the sprite, the oldest listing time that is still open. */
        fun openListingsSql(kind: Int): String =
            "SELECT COUNT(*) AS open_offers, COALESCE(MIN(l.price), 0) AS cheapest" +
                LISTINGS_OF_SPRITE + kindFilter(kind) +
                " AND l.state = $OPEN AND l.listed_at > ?"

        fun utcDay(now: Int): Int = now / SECONDS_PER_DAY

        fun firstSecondOfDaysAgo(
            now: Int,
            daysAgo: Int,
        ): Int = (utcDay(now) - daysAgo) * SECONDS_PER_DAY
    }
}
