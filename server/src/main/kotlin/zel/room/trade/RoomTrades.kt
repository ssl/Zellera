package zel.room.trade

import org.slf4j.LoggerFactory
import zel.core.rules.TradeCreditFurni
import zel.game.achievements.GameEvents
import zel.game.economy.EconomyLedger
import zel.game.items.CreditFurni
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.protocol.generated.FurniListRemoveMultipleOut
import zel.protocol.generated.FurniListRemoveMultipleOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.InventoryFurni
import zel.protocol.outgoing.inventory.InventoryUpdateItem
import zel.protocol.outgoing.users.UserCredits
import zel.protocol.trading.TradeClosed
import zel.protocol.trading.TradeMessages
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.unit.RoomUnit
import zel.room.unit.walk.Walking
import java.util.concurrent.ConcurrentHashMap

/** Every member runs under the room's lock. */
class RoomTrades(
    private val walking: Walking,
    private val messages: TradeMessages,
    private val store: TradeStore,
    private val creditFurni: () -> TradeCreditFurni,
    private val progress: (playerId: Int, event: String, amount: Int) -> Unit,
    private val nowSeconds: () -> Int,
    private val persist: (Runnable) -> Unit,
    private val post: (Int, (RoomScope) -> Unit) -> Unit,
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, MutableList<Trade>>()

    override fun load(room: Room) {
        rooms[room] = ArrayList()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun tradeOf(
        room: Room,
        playerId: Int,
    ): Trade? = rooms[room]?.firstOrNull { it.sideOf(playerId) != null }

    fun tradeOfUnit(
        room: Room,
        unitIndex: Int,
    ): Trade? = rooms[room]?.firstOrNull { it.holds(unitIndex) }

    fun start(
        scope: RoomScope,
        first: Trade.Side,
        second: Trade.Side,
    ) {
        val trade = Trade(first, second)
        rooms.getOrPut(scope.room) { ArrayList() } += trade
        trade.sides.forEach { side ->
            side.player.client?.let { messages.open(it, first.id, second.id) }
        }
        trade.sides.forEach { showStatus(scope, it, trading = true) }
    }

    fun offer(
        trade: Trade,
        side: Trade.Side,
        items: Collection<OwnedFurni>,
    ) {
        side.items += items
        trade.reopen()
        showOffers(trade)
    }

    fun takeBack(
        trade: Trade,
        side: Trade.Side,
        item: OwnedFurni,
    ) {
        side.items -= item
        trade.reopen()
        showOffers(trade)
    }

    fun accept(
        trade: Trade,
        side: Trade.Side,
        accepted: Boolean,
    ) {
        side.accepted = accepted
        if (!accepted) trade.sides.forEach { it.confirmed = false }
        trade.sides.forEach { other ->
            other.player.client?.let { messages.accepted(it, side.id, accepted) }
        }
        if (trade.bothAccepted) {
            trade.sides.forEach { other -> other.player.client?.let(messages::waitingConfirm) }
        }
    }

    fun confirm(
        scope: RoomScope,
        trade: Trade,
        side: Trade.Side,
    ) {
        if (!side.accepted) return
        side.confirmed = true
        if (trade.bothConfirmed) settle(scope, trade)
    }

    fun close(
        scope: RoomScope,
        trade: Trade,
        byPlayerId: Int,
    ) {
        forget(scope, trade)
        trade.sides.forEach { side ->
            side.player.client?.let { messages.closed(it, byPlayerId, TradeClosed.CANCELLED) }
        }
        trade.sides.forEach { it.items.clear() }
        showOffers(trade)
        trade.sides.forEach { showStatus(scope, it, trading = false) }
    }

    fun left(
        scope: RoomScope,
        unitIndex: Int,
    ) {
        val trade = tradeOfUnit(scope.room, unitIndex) ?: return
        val gone = trade.sides.first { it.unitIndex == unitIndex }
        forget(scope, trade)
        trade.sides.filter { it !== gone }.forEach { other ->
            other.player.client?.let { messages.closed(it, gone.id, TradeClosed.CANCELLED) }
            showStatus(scope, other, trading = false)
        }
    }

    private fun settle(
        scope: RoomScope,
        trade: Trade,
    ) {
        val first = trade.first
        val second = trade.second
        forget(scope, trade)
        if (trade.sides.all { it.items.isEmpty() } || !takeOffers(trade)) {
            endWithoutMoving(scope, trade)
            return
        }
        val redeemed =
            if (creditFurni() == TradeCreditFurni.WALLET) {
                trade.sides
                    .flatMap { it.items }
                    .associate { it.id to CreditFurni.credits(it.definition) }
                    .filterValues { it > 0 }
            } else {
                emptyMap()
            }
        val record =
            TradeStore.Settled(
                TradeStore.Settled.moves(first.items, second.id) + TradeStore.Settled.moves(second.items, first.id),
                first.id,
                second.id,
                first.player.profile.lastIp,
                second.player.profile.lastIp,
                nowSeconds(),
                redeemed,
            )
        val roomId = scope.room.info.id
        persist(
            Runnable {
                val moved = written(trade, record)
                post(roomId) { inner -> ended(inner, trade, moved) }
            },
        )
    }

    /** Runs off the room's lock; false when nothing moved. */
    private fun written(
        trade: Trade,
        record: TradeStore.Settled,
    ): Boolean {
        val first = trade.first
        val second = trade.second
        val redeemed = record.redeemed
        val credited = saved(record, first.player, second.player)
        if (credited == null) {
            trade.sides.forEach {
                it.player.inventory.furni
                    .putBack(it.items)
            }
            return false
        }
        hand(first.items, second.player, redeemed.keys)
        hand(second.items, first.player, redeemed.keys)
        announce(first.items, second.items.filter { it.id !in redeemed }, first.player)
        announce(second.items, first.items.filter { it.id !in redeemed }, second.player)
        trade.sides
            .filter { it.id in credited }
            .forEach { side -> side.player.client?.send(UserCredits(side.player)) }
        trade.sides.forEach { side ->
            side.player.client?.let {
                messages.completed(it)
                messages.closed(it, side.id, TradeClosed.COMPLETED)
            }
            progress(side.id, GameEvents.TRADE_DONE, 1)
            val coins = side.items.sumOf { CreditFurni.credits(it.definition) }
            if (coins > 0) progress(side.id, GameEvents.TRADE_COIN_VALUE, coins)
        }
        return true
    }

    /** Back on the room's thread: the trading status goes, unless a side already started another trade. */
    private fun ended(
        scope: RoomScope,
        trade: Trade,
        moved: Boolean,
    ) {
        val free = trade.sides.filter { tradeOf(scope.room, it.id) == null }
        if (!moved) {
            trade.sides.forEach { side ->
                side.player.client?.let { messages.closed(it, side.id, TradeClosed.NOTHING_MOVED) }
            }
        }
        free.forEach { showStatus(scope, it, trading = false) }
    }

    @Suppress("TooGenericExceptionCaught") // whatever stops the write, nothing moves
    private fun saved(
        record: TradeStore.Settled,
        vararg players: Player,
    ): Map<Int, Int>? =
        try {
            if (record.redeemed.isEmpty()) store.settle(record) else settleUnderWallets(record, players.toList())
        } catch (e: Exception) {
            log.error("trade between {} and {} could not be saved", record.firstId, record.secondId, e)
            null
        }

    /** Holds both wallets, lowest id first, so the new balances reach memory in the same step as the database. */
    private fun settleUnderWallets(
        record: TradeStore.Settled,
        players: List<Player>,
    ): Map<Int, Int>? {
        val (low, high) = players.sortedBy { it.profile.id }
        return WalletWrites.locked(low) {
            WalletWrites.locked(high) {
                store.settle(record)?.also { balances ->
                    players.filter { it.profile.id in balances }.forEach { player ->
                        WalletWrites.publish(
                            player,
                            EconomyLedger.CREDITS,
                            balances.getValue(player.profile.id),
                        )
                    }
                }
            }
        }
    }

    /** Takes both offers out of the inventories before the write; false, taking nothing, if a side lacks its offer. */
    private fun takeOffers(trade: Trade): Boolean {
        val taken = ArrayList<Trade.Side>()
        for (side in trade.sides) {
            if (side.items.isNotEmpty() &&
                !side.player.inventory.furni
                    .takeAll(side.items)
            ) {
                taken.forEach {
                    it.player.inventory.furni
                        .putBack(it.items)
                }
                return false
            }
            taken += side
        }
        return true
    }

    private fun endWithoutMoving(
        scope: RoomScope,
        trade: Trade,
    ) {
        trade.sides.forEach { side ->
            side.player.client?.let { messages.closed(it, side.id, TradeClosed.NOTHING_MOVED) }
        }
        trade.sides.forEach { showStatus(scope, it, trading = false) }
    }

    private fun hand(
        items: Collection<OwnedFurni>,
        to: Player,
        redeemed: Set<Int>,
    ) {
        val stash = to.inventory.furniOrNull ?: return
        items.filter { it.id !in redeemed }.forEach { item ->
            item.userId = to.profile.id
            stash.put(item)
        }
    }

    private fun announce(
        given: Collection<OwnedFurni>,
        received: Collection<OwnedFurni>,
        player: Player,
    ) {
        val client = player.client ?: return
        if (given.isNotEmpty()) {
            client.send(
                FurniListRemoveMultipleOutCodec,
                FurniListRemoveMultipleOut(given.map { InventoryFurni.entry(it).stripId }),
            )
        }
        if (received.isNotEmpty()) {
            client.send(InventoryUpdateItem(received))
            client.send(AddRoomItem(received))
        }
    }

    private fun forget(
        scope: RoomScope,
        trade: Trade,
    ) {
        rooms[scope.room]?.remove(trade)
    }

    private fun showOffers(trade: Trade) {
        val offers = trade.sides.map { it.id to it.items.toList() }
        trade.sides.forEach { side ->
            side.player.client?.let { client ->
                messages.offers(client, offers.sortedByDescending { it.first == side.id })
            }
        }
    }

    private fun showStatus(
        scope: RoomScope,
        side: Trade.Side,
        trading: Boolean,
    ) {
        val unit: RoomUnit = scope.room.units[side.unitIndex] ?: return
        if (trading) unit.setStatus(STATUS) else unit.dropStatus(STATUS)
        walking.sendNow(scope, listOf(unit))
    }

    companion object {
        private val log = LoggerFactory.getLogger(RoomTrades::class.java)
        const val STATUS = "trd"
    }
}
