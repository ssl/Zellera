package zel.game.catalog

import com.zaxxer.hikari.HikariDataSource
import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.db.Database
import zel.game.Game
import zel.game.achievements.Achievements
import zel.game.bots.Bots
import zel.game.catalog.marketplace.Marketplace
import zel.game.catalog.marketplace.MarketplaceOfferCache
import zel.game.catalog.marketplace.SaleSettlement
import zel.game.earnings.IncomeRewards
import zel.game.items.Items
import zel.game.moderation.WordFilter
import zel.game.pets.PetsModule
import zel.game.players.ChatStyles
import zel.game.players.Players
import zel.game.players.inventory.BadgeCaches
import zel.net.clients.ClientConnections
import zel.room.port.RoomPort
import zel.util.MILLIS_PER_SECOND
import java.util.Random

class CatalogServices(
    val game: Game,
    val config: Configuration,
    val texts: Texts,
    val threading: GameThreads,
    val database: Database,
    val random: Random,
    private val unixTime: () -> Int,
    private val shutdownAt: () -> Int = { 0 },
    private val connections: () -> ClientConnections? = { null },
) {
    val settings = CatalogSettings(config)
    val offerCache = MarketplaceOfferCache()
    val recyclerCooldown = RecyclerCooldown { now() * MILLIS_PER_SECOND }
    val marketplace: Marketplace by lazy { Marketplace(this, SaleSettlement(database)) }
    val raffles: ReleaseRaffles by lazy { ReleaseRaffles(this) }

    fun now(): Int = unixTime()

    /** Unix seconds; 0 while no shutdown is scheduled. */
    fun shutdownTimestamp(): Int = shutdownAt()

    fun clients(): ClientConnections? = connections()

    val dataSource: HikariDataSource get() = database.dataSource

    val catalog: Catalog get() = started(game.catalog, "catalog")
    val items: Items get() = started(game.items, "items")
    val bots: Bots get() = started(game.bots, "bots")
    val pets: PetsModule get() = started(game.pets, "pets")
    val players: Players get() = started(game.players, "players")
    val achievements: Achievements get() = started(game.achievements, "achievements")
    val wordFilter: WordFilter get() = started(game.wordFilter, "word filter")
    val incomeRewards: IncomeRewards get() = started(game.incomeRewards, "income rewards")
    val rooms: RoomPort get() = game.rooms
    val chatStyles: ChatStyles get() = game.chatStyles
    val badgeCaches: BadgeCaches get() = game.badgeCaches

    private fun <T : Any> started(
        module: T?,
        name: String,
    ): T = module ?: error("the $name are not loaded yet")
}
