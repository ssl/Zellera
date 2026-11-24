package zel.protocol.incoming.catalog.recycler

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.achievements.AchievementTally
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.catalog.CatalogServices
import zel.game.catalog.RecyclerAchievements
import zel.game.catalog.RecyclerCooldown
import zel.game.catalog.RecyclerPrizes
import zel.game.economy.BalanceChange
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.economy.LedgerTransaction
import zel.game.items.OwnedFurni
import zel.game.items.exchangeInventoryFurni
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.game.players.inventory.FurniStash
import zel.net.clients.ClientConnection
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.generated.FurniListRemoveMultipleOut
import zel.protocol.generated.FurniListRemoveMultipleOutCodec
import zel.protocol.generated.InfoGameClosingOut
import zel.protocol.generated.InfoGameClosingOutCodec
import zel.protocol.generated.RecycleItemsIn
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.RecyclerResult
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.InventoryUpdateItem
import zel.util.SECONDS_PER_MINUTE

class Recycling(
    private val services: CatalogServices,
) {
    fun recyclerSlotCount(): Int =
        services.config.getInt("recycler.furni_per_prize", DEFAULT_SLOT_COUNT).coerceIn(1, MAX_SLOT_COUNT)

    fun handle(
        client: ClientConnection,
        m: RecycleItemsIn,
    ) {
        if (services.shutdownTimestamp() > 0) {
            val minutes = (services.shutdownTimestamp() - services.now()) / SECONDS_PER_MINUTE
            client.send(InfoGameClosingOutCodec, InfoGameClosingOut(minutes))
            return
        }
        if (services.catalog.recycler.boxItem != null && services.items.recyclerEnabled) {
            recycle(client, m)
        } else {
            CatalogReplies.recyclerFinished(client, RecyclerResult.CLOSED)
        }
    }

    private fun recycle(
        client: ClientConnection,
        m: RecycleItemsIn,
    ) {
        val player = checkNotNull(client.player) { "a recycle request needs a player" }
        val userId = player.profile.id
        // RecycleItems only comes from inside a private room.
        if (!player.inRoom) return
        if (services.recyclerCooldown.waiting(userId)) {
            CatalogReplies.recyclerFinished(client, RecyclerResult.CLOSED, -1)
            return
        }
        // the client offers neither a short run nor one the player cannot pay, so those get no answer
        if (m.itemIds.size != recyclerSlotCount() || player.profile.points < RecyclerCooldown.POINTS_COST) return
        val furni = player.inventory.furni
        val items = m.itemIds.mapNotNullTo(LinkedHashSet()) { furni[it] }
        val allRecyclable = items.size == m.itemIds.size && items.all { it.definition.canRecycle }
        if (!allRecyclable || !furni.takeAll(items)) {
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return
        }
        exchange(client, player, furni, items)
    }

    private fun exchange(
        client: ClientConnection,
        player: Player,
        furni: FurniStash,
        items: Set<OwnedFurni>,
    ) {
        val userId = player.profile.id
        var consumed = false
        try {
            val catalog = services.catalog
            val drawn = catalog.recycler.draw()
            val settled = drawn?.let { settle(player, items, it.prize) }
            if (drawn == null || settled == null) {
                CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
                return
            }
            consumed = true
            val (reward, charge) = settled
            furni.put(reward)

            // Wire order: box added, strips removed, box updated, point charge, result with the prize's strip id.
            client.send(AddRoomItem(reward))
            client.send(FurniListRemoveMultipleOutCodec, FurniListRemoveMultipleOut(items.map { it.stripId }))
            client.send(InventoryUpdateItem(reward))
            client.send(
                ActivityPointNotificationOutCodec,
                ActivityPointNotificationOut(
                    charge.after,
                    charge.after - charge.before,
                    EconomyLedger.POINTS,
                ),
            )
            services.recyclerCooldown.ran(userId)
            CatalogReplies.recyclerFinished(client, RecyclerResult.DONE, reward.stripId)

            checkNotNull(services.game.achievements).count(player, GameEvents.RECYCLER_USED)
            RecyclerAchievements.recycled(
                player,
                items.map { it.definition },
                catalog.contents.recyclerPrizesByLevel(),
                drawn.level,
                AchievementTally.running(),
            )
        } finally {
            if (!consumed) furni.putBack(items)
        }
    }

    @Suppress("TooGenericExceptionCaught") // any failure means nothing recycled
    private fun settle(
        player: Player,
        items: Set<OwnedFurni>,
        prize: RecyclerPrizes.Entry,
    ): Pair<OwnedFurni, BalanceChange>? {
        val userId = player.profile.id
        val debit =
            LedgerEntry(
                LedgerEntry.newKey("recycler/$userId"),
                userId,
                EconomyLedger.POINTS,
                -RecyclerCooldown.POINTS_COST,
                "recycler.fee",
            )
        return try {
            WalletWrites.locked(player) {
                services.database.dataSource
                    .exchangeInventoryFurni(userId, items) { connection ->
                        services.items.prizeBoxes
                            .recycle(connection, player, prize)
                            ?.let { box -> box to LedgerTransaction(connection).post(debit) }
                    }?.also { (_, charge) -> WalletWrites.publish(player, EconomyLedger.POINTS, charge.after) }
            }
        } catch (e: Exception) {
            log.info("Recycling of player {} refused: {}", userId, e.message)
            null
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Recycling::class.java)
        const val DEFAULT_SLOT_COUNT = 8
        const val MAX_SLOT_COUNT = 12
    }
}
