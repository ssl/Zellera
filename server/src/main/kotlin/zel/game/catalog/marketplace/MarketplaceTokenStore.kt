package zel.game.catalog.marketplace

import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import javax.sql.DataSource

/** Written at once, so a charge never outlives its tokens. */
fun interface MarketplaceTokenStore {
    fun add(
        playerId: Int,
        delta: Int,
    ): Boolean

    companion object {
        private val log = LoggerFactory.getLogger(MarketplaceTokenStore::class.java)

        private const val ADD_SQL =
            "UPDATE player_stats SET marketplace_tokens = GREATEST(marketplace_tokens + ?, 0) WHERE player_id = ?"

        fun jdbc(dataSource: () -> DataSource): MarketplaceTokenStore =
            MarketplaceTokenStore { playerId, delta ->
                log.orFallback("adding $delta marketplace tokens to player $playerId", false) {
                    Sql(dataSource()).write(ADD_SQL, delta, playerId) == 1
                }
            }
    }
}
