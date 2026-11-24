package zel.wired.chest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.game.achievements.GameEvents
import zel.game.earnings.WiredCreditEarnings
import zel.game.economy.EconomyLedger
import zel.game.items.Item
import zel.game.items.ItemServices
import zel.game.items.OwnedFurni
import zel.game.items.ProductKind
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.game.players.inventory.FurniStash
import zel.net.clients.ClientConnections
import zel.protocol.generated.FurniListAddOrUpdateOut
import zel.protocol.generated.FurniListAddOrUpdateOutCodec
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.UnseenItemsOut
import zel.protocol.generated.UnseenItemsOutCodec
import zel.protocol.outgoing.inventory.InventoryFurni
import zel.protocol.outgoing.users.UserCredits
import zel.room.RoomScope
import zel.wired.store.WiredWriter
import zel.wired.timing.RoomPoster
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

@Suppress("LongParameterList") // the server services a chest reaches
class GameChests(
    private val clients: ClientConnections,
    private val config: Configuration,
    private val store: ChestStore,
    private val writer: WiredWriter,
    private val items: () -> ItemServices?,
    private val earnings: () -> WiredCreditEarnings?,
    private val progress: (Int, String, Int) -> Unit,
    private val poster: RoomPoster,
    private val filterText: (Int, String) -> String = { _, text -> text },
    private val later: (playerId: Int, job: Runnable) -> Unit = { _, job -> job.run() },
) : ChestHost {
    private val onTables = ConcurrentHashMap<Int, OwnedFurni>()

    override fun isOnline(playerId: Int): Boolean = clients.onlinePlayer(playerId) != null

    override fun name(playerId: Int): String =
        clients
            .onlinePlayer(playerId)
            ?.profile
            ?.name
            .orEmpty()

    override fun announceEarning(
        playerId: Int,
        category: Int,
    ) {
        earnings()?.announce(playerId, category)
    }

    override fun mayTrade(playerId: Int): Boolean =
        clients
            .onlinePlayer(playerId)
            ?.stats
            ?.standing
            ?.mayTrade() ?: store.mayTrade(playerId)

    override fun offered(
        playerId: Int,
        itemId: Int,
    ): OfferedFurni? {
        val item = inventory(playerId)?.get(itemId) ?: return null
        val base = item.definition
        val entry = InventoryFurni.entry(item)
        return OfferedFurni(
            id = item.id,
            definitionId = base.id,
            spriteId = base.spriteId,
            wall = base.type == ProductKind.WALL_FURNI,
            name = base.name,
            state = item.state,
            limitedTotal = item.editionSize,
            limitedNumber = item.editionNumber,
            tradable = base.canTrade,
            stuff = entry.stuffData,
            category = entry.category,
            extra = entry.floorExtra ?: 0,
        )
    }

    override fun take(
        playerId: Int,
        itemId: Int,
    ): Boolean {
        val item = inventory(playerId)?.takeOut(itemId) ?: return false
        onTables[item.id] = item
        return true
    }

    override fun putBack(
        playerId: Int,
        itemIds: List<Int>,
    ) {
        val inventory = inventory(playerId)
        itemIds.forEach { id -> onTables.remove(id)?.let { inventory?.put(it) } }
    }

    override fun refreshInventory(playerId: Int) = send(playerId, FurniListInvalidateOutCodec, FurniListInvalidateOut)

    override fun settle(
        roomId: Int,
        settlement: ChestSettlement,
        undo: (RoomScope) -> Unit,
    ) {
        writer.write(
            roomId,
            Runnable {
                val written = written(settlement)
                if (written == null) {
                    putBackPaid(settlement)
                    poster.post(roomId, 0, undo)
                } else {
                    settlement.paid.forEach(onTables::remove)
                    deliver(written.created)
                    paidOut(settlement, written.purses)
                }
            },
        )
    }

    /** Holds the online receivers' wallets lowest id first; null, logged, on failure. */
    @Suppress("TooGenericExceptionCaught") // whatever stops the write, nothing of the move stands
    private fun written(settlement: ChestSettlement): ChestSettlement.Written<OwnedFurni>? {
        val wallets =
            settlement.payouts
                .filter { it.category == WiredCreditEarnings.PURSE }
                .map { it.playerId }
                .distinct()
                .sorted()
                .mapNotNull(clients::onlinePlayer)
        return try {
            underWallets(wallets) {
                store.settle(settlement, ::newFurni).also { written ->
                    wallets.forEach { player ->
                        written.purses[player.profile.id]?.let {
                            WalletWrites.publish(player, EconomyLedger.CREDITS, it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("chest move of transactions {} could not be saved", settlement.rows.map { it.id }, e)
            null
        }
    }

    private fun <T> underWallets(
        players: List<Player>,
        work: () -> T,
    ): T =
        if (players.isEmpty()) {
            work()
        } else {
            WalletWrites.locked(players.first()) { underWallets(players.drop(1), work) }
        }

    private fun putBackPaid(settlement: ChestSettlement) {
        if (settlement.paid.isEmpty()) return
        putBack(settlement.payerId, settlement.paid)
        refreshInventory(settlement.payerId)
    }

    private fun paidOut(
        settlement: ChestSettlement,
        purses: Map<Int, Int>,
    ) {
        purses.keys.forEach { playerId ->
            clients.onlinePlayer(playerId)?.let { player -> player.client?.send(UserCredits(player)) }
        }
        settlement.payouts
            .filter { it.category != WiredCreditEarnings.PURSE && !it.announced }
            .forEach { earnings()?.announce(it.playerId, it.category) }
    }

    private fun newFurni(
        connection: Connection,
        playerId: Int,
        stored: StoredFurni,
    ): OwnedFurni? {
        val manager = items()?.items() ?: return null
        return manager.newFurni(
            connection,
            playerId,
            manager.getItem(stored.definitionId),
            stored.limitedTotal,
            stored.limitedNumber,
            stored.state,
        )
    }

    private fun deliver(created: List<Pair<Int, OwnedFurni>>) {
        created.groupBy({ it.first }, { it.second }).forEach { (playerId, furni) ->
            val inventory = inventory(playerId) ?: return@forEach
            inventory.putAll(furni)
            val entries = furni.map(::entry)
            send(playerId, FurniListAddOrUpdateOutCodec, FurniListAddOrUpdateOut(entries))
            val seen = UnseenItemsOut.CategoriesItem(UNSEEN_FURNI, entries.map { it.stripId })
            send(playerId, UnseenItemsOutCodec, UnseenItemsOut(listOf(seen)))
        }
    }

    private fun entry(item: OwnedFurni): FurniListAddOrUpdateOut.ItemsItem {
        val e = InventoryFurni.entry(item)
        return FurniListAddOrUpdateOut.ItemsItem(
            stripId = e.stripId,
            type = e.type,
            itemId = e.itemId,
            spriteId = e.spriteId,
            category = e.category,
            stuffData = e.stuffData,
            recyclable = e.recyclable,
            tradable = e.tradable,
            groupable = e.groupable,
            sellable = e.sellable,
            secondsToExpiration = InventoryFurni.NEVER_EXPIRES,
            rentPeriodStarted = false,
            roomId = NOT_IN_ROOM,
            floorExtras =
                e.floorExtra?.let { FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Then("", it) }
                    ?: FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Else,
        )
    }

    override fun progress(
        playerId: Int,
        achievement: String,
        amount: Int,
    ) = progress.invoke(playerId, achievement, amount)

    override fun furniChestsInUse(
        playerId: Int,
        count: Int,
    ) {
        val player = clients.onlinePlayer(playerId) ?: return
        val achievements = player.services.game.achievements ?: return
        later(playerId) {
            for (achievement in achievements.forEvent(GameEvents.CHEST_ORGANIZED)) {
                val shown = maxOf(0, player.stats.progressOf(achievement))
                if (count > shown) achievements.progress(player, achievement, count - shown)
            }
        }
    }

    override fun definition(
        wall: Boolean,
        spriteId: Int,
    ): Item? {
        val type = if (wall) ProductKind.WALL_FURNI else ProductKind.FLOOR_FURNI
        return items()
            ?.items()
            ?.getItems()
            ?.values
            ?.firstOrNull { it.spriteId == spriteId && it.type == type }
    }

    override fun spriteOf(baseId: Int): Int = items()?.items()?.getItem(baseId)?.spriteId ?: baseId

    override fun isWall(baseId: Int): Boolean = items()?.items()?.getItem(baseId)?.type == ProductKind.WALL_FURNI

    override fun credits(playerId: Int): Int = clients.onlinePlayer(playerId)?.profile?.credits ?: 0

    override fun premium(playerId: Int): Int =
        clients.onlinePlayer(playerId)?.profile?.balance(EconomyLedger.PREMIUM) ?: 0

    override fun takeCredits(
        playerId: Int,
        amount: Int,
    ) {
        clients.onlinePlayer(playerId)?.wallet?.changeCredits(-amount)
    }

    override fun takePremium(
        playerId: Int,
        amount: Int,
    ) {
        clients.onlinePlayer(playerId)?.wallet?.changePoints(EconomyLedger.PREMIUM, -amount)
    }

    override fun <T> withWallet(
        playerId: Int,
        work: () -> T,
    ): T = clients.onlinePlayer(playerId)?.let { WalletWrites.locked(it, work) } ?: work()

    override fun <T> send(
        playerId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    ) {
        clients.onlinePlayer(playerId)?.client?.send(codec, message)
    }

    override fun filtered(
        playerId: Int,
        text: String,
    ): String = filterText(playerId, text)

    override fun starterTexts(): Pair<String, String> =
        config.getValue(STARTER_NAME, "") to config.getValue(STARTER_DESCRIPTION, "")

    private fun inventory(playerId: Int): FurniStash? {
        val player = clients.onlinePlayer(playerId) ?: return null
        return try {
            player.inventory.furni
        } catch (notLoaded: IllegalStateException) {
            log.warn("chest furni of player {} skip an inventory that is not loaded", playerId, notLoaded)
            null
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GameChests::class.java)
        const val NOT_IN_ROOM = -1
        const val UNSEEN_FURNI = 1
        const val STARTER_NAME = "wired.chest.starter.name"
        const val STARTER_DESCRIPTION = "wired.chest.starter.description"
    }
}
