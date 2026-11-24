package zel.wired.chest

import zel.game.achievements.GameEvents
import zel.game.earnings.WiredCreditEarnings
import zel.game.items.CreditFurni
import zel.protocol.generated.ItemsChestContentsUpdatedOutCodec
import zel.protocol.generated.WiredTradeCancelledOut
import zel.protocol.generated.WiredTradeCancelledOutCodec
import zel.protocol.generated.WiredTradeCompletedOut
import zel.protocol.generated.WiredTradeCompletedOutCodec
import zel.protocol.generated.WiredTradeInitiateOut
import zel.protocol.generated.WiredTradeInitiateOutCodec
import zel.protocol.generated.WiredTradeItemsUpdateOutCodec
import zel.protocol.generated.WiredTradeTransactionNotificationOut
import zel.protocol.generated.WiredTradeTransactionNotificationOutCodec
import zel.protocol.generated.WiredTransactionFailOut
import zel.protocol.generated.WiredTransactionFailOutCodec
import zel.protocol.generated.WiredTransactionSuccessOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

class ChestDeals(
    private val chests: RoomChests,
    private val moves: ChestMoves,
    private val notices: ChestNotices,
    private val host: ChestHost,
    private val triggers: TransactionTriggers,
    private val clock: () -> Long = System::currentTimeMillis,
) : RoomPart {
    private val tables = ConcurrentHashMap<Room, HashMap<Int, Table>>()

    override fun unload(scope: RoomScope) {
        tables.remove(scope.room)?.values?.forEach { table ->
            host.putBack(table.playerId, table.offered.keys.toList())
        }
    }

    val tick: TickPart = TickPart { scope -> expire(scope) }

    private fun expire(scope: RoomScope) {
        val open = tables[scope.room] ?: return
        val now = clock()
        open.values
            .filter { table ->
                val seconds = (table.deal as? Deal.Negotiated)?.timeoutSeconds ?: 0
                seconds > 0 && now - table.startedAt >= seconds * MILLIS_PER_SECOND + TIMEOUT_GRACE
            }.forEach { cancel(scope, it, CANCEL_TIMEOUT, REASON_USER) }
    }

    fun table(
        room: Room,
        playerId: Int,
    ): Table? = tables[room]?.get(playerId)

    private fun tablesOf(room: Room): HashMap<Int, Table> = tables.computeIfAbsent(room) { HashMap() }

    fun startDeposit(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        chestId: Int,
    ) {
        val (furni, chest) = chests.inRoom(scope.room, chestId) ?: return
        when {
            !host.mayTrade(playerId) -> fail(scope, playerId, FAIL_CANT_TRADE)
            table(scope.room, playerId) != null -> fail(scope, playerId, FAIL_NEGOTIATING)
            !mayDeposit(furni, chest, playerId, level) -> Unit
            !host.mayTrade(furni.ownerId) -> fail(scope, playerId, FAIL_OWNER_CANT_TRADE)
            chest.contents >= chest.capacity -> fail(scope, playerId, FAIL_CAPACITY)
            else -> open(scope, Table(playerId, Deal.Deposit(chestId), clock()), TableWire.depositInitiate(chest))
        }
    }

    private fun mayDeposit(
        furni: RoomFurni,
        chest: Chest,
        playerId: Int,
        level: Int,
    ): Boolean =
        furni.ownerId == playerId || (!chest.locked && (chest.preferences.everyoneDonates || level >= DoorCheck.RIGHTS))

    fun open(
        scope: RoomScope,
        table: Table,
        initiate: WiredTradeInitiateOut,
    ) {
        tablesOf(scope.room)[table.playerId] = table
        val to = Audience.Player(table.playerId)
        scope.out.send(Lane.Now, to, WiredTradeInitiateOutCodec, initiate)
        update(scope, table)
    }

    fun offer(
        scope: RoomScope,
        playerId: Int,
        remove: Boolean,
        ids: List<Int>,
    ) {
        val table = table(scope.room, playerId) ?: return
        val itemIds = ids.map { it.absoluteValue }.distinct()
        if (remove) takeBack(scope, table, itemIds) else add(scope, table, itemIds)
    }

    private fun takeBack(
        scope: RoomScope,
        table: Table,
        ids: List<Int>,
    ) {
        val back = ids.filter { table.offered.remove(it) != null }
        host.putBack(table.playerId, back)
        table.acceptedAt = null
        host.refreshInventory(table.playerId)
        update(scope, table)
    }

    private fun add(
        scope: RoomScope,
        table: Table,
        ids: List<Int>,
    ) {
        var added = false
        var refused = false
        ids.filterNot { it in table.offered }.forEach { id ->
            val item = host.offered(table.playerId, id)
            val error = refusal(scope.room, table, item)
            when {
                error != null -> {
                    refused = true
                    tradeError(scope, table.playerId, error)
                }

                item != null && host.take(table.playerId, id) -> {
                    table.offered[id] = item
                    added = true
                }

                else -> {
                    refused = true
                    tradeError(scope, table.playerId, ERROR_INVALID)
                }
            }
        }
        if (added) table.acceptedAt = null
        if (added) host.refreshInventory(table.playerId)
        if (added || !refused) update(scope, table)
    }

    private fun refusal(
        room: Room,
        table: Table,
        item: OfferedFurni?,
    ): Int? {
        if (item == null || !item.tradable) return ERROR_INVALID
        return when (val deal = table.deal) {
            is Deal.Deposit -> depositRefusal(room, table, deal, item)
            is Deal.Negotiated -> dealRefusal(room, table, deal, item)
        }
    }

    private fun depositRefusal(
        room: Room,
        table: Table,
        deal: Deal.Deposit,
        item: OfferedFurni,
    ): Int? {
        val chest = chests.inRoom(room, deal.chestId)?.second ?: return ERROR_INVALID
        val value = CreditFurni.credits(item.name)
        val onTable =
            if (chest.isCoins) table.offered.values.sumOf { CreditFurni.credits(it.name) } else table.offered.size
        return when {
            chest.isCoins && value == 0 -> ERROR_INVALID
            chest.contents + onTable + (if (chest.isCoins) value else 1) > chest.capacity -> ERROR_CAPACITY
            else -> null
        }
    }

    private fun dealRefusal(
        room: Room,
        table: Table,
        deal: Deal.Negotiated,
        item: OfferedFurni,
    ): Int? {
        val coinsOnly = chests.wiredOf(room, deal.chestIds).all { it.second.isCoins }
        return when {
            deal.contract.isDonation -> {
                if (coinsOnly && CreditFurni.credits(item.name) == 0) ERROR_INVALID else null
            }

            Requirements.covered(deal.contract, table.offered.values).second >= Requirements.limit(deal) -> {
                ERROR_ENOUGH
            }

            else -> {
                null
            }
        }
    }

    private fun update(
        scope: RoomScope,
        table: Table,
    ) {
        val (canAccept, sets) = offerState(table)
        val message = TableWire.itemsUpdate(table, canAccept, sets)
        scope.out.send(Lane.Now, Audience.Player(table.playerId), WiredTradeItemsUpdateOutCodec, message)
    }

    private fun canAccept(
        table: Table,
        sets: Int,
    ): Boolean {
        val deal = table.deal
        return if (deal is Deal.Negotiated && !deal.contract.isDonation) {
            sets >= Requirements.needed(deal)
        } else {
            table.offered.isNotEmpty()
        }
    }

    fun offerState(table: Table): Pair<Boolean, Int> {
        val deal = table.deal
        val sets =
            if (deal is Deal.Negotiated && !deal.contract.isDonation) {
                Requirements.covered(deal.contract, table.offered.values).second
            } else {
                0
            }
        return canAccept(table, sets) to sets
    }

    fun confirm(
        scope: RoomScope,
        playerId: Int,
        accept: Boolean,
    ) {
        val table = table(scope.room, playerId) ?: return
        when {
            !accept -> table.acceptedAt = clock()
            !offerState(table).first || table.acceptedAt == null -> cancel(scope, table, CANCEL_UNMET, REASON_INVALID)
            else -> settle(scope, table)
        }
    }

    fun cancelled(
        scope: RoomScope,
        playerId: Int,
    ) {
        table(scope.room, playerId)?.let { cancel(scope, it, CANCEL_USER, REASON_USER) }
    }

    fun left(
        scope: RoomScope,
        playerId: Int,
    ) {
        table(scope.room, playerId)?.let { cancel(scope, it, CANCEL_USER, REASON_LEFT) }
    }

    fun chestGone(
        scope: RoomScope,
        chestId: Int,
    ) {
        tablesOf(scope.room).values.filter { chestId in it.deal.chestIds }.forEach {
            cancel(scope, it, CANCEL_USER, REASON_USER)
        }
    }

    fun cancelByWired(
        scope: RoomScope,
        playerId: Int,
        matches: (Int) -> Boolean,
    ) {
        val table = table(scope.room, playerId) ?: return
        if (table.deal is Deal.Negotiated && matches(table.contractId)) {
            cancel(scope, table, CANCEL_BY_WIRED, CANCEL_BY_WIRED)
        }
    }

    fun cancel(
        scope: RoomScope,
        table: Table,
        cancelCode: Int,
        reason: Int,
        failTriggers: Boolean = true,
    ) {
        tablesOf(scope.room).remove(table.playerId)
        val back = table.offered.keys.toList()
        host.putBack(table.playerId, back)
        if (back.isNotEmpty()) host.refreshInventory(table.playerId)
        scope.out.send(
            Lane.Now,
            Audience.Player(table.playerId),
            WiredTradeCancelledOutCodec,
            WiredTradeCancelledOut(cancelCode),
        )
        if (failTriggers && table.deal is Deal.Negotiated) {
            triggers.failed(
                scope,
                table.contractId,
                scope.room.units
                    .ofPlayer(table.playerId)
                    ?.index,
                reason,
            )
        }
    }

    private fun settle(
        scope: RoomScope,
        table: Table,
    ) {
        tradeBar(scope.room, table)?.let { reason ->
            cancel(scope, table, CANCEL_USER, reason)
            fail(scope, table.playerId, reason)
            return
        }
        when (val deal = table.deal) {
            is Deal.Deposit -> settleDeposit(scope, table, deal)
            is Deal.Negotiated -> settleDeal(scope, table, deal)
        }
    }

    /** A trade lock can start while the table is open; the move itself asks again, for the user and every owner. */
    private fun tradeBar(
        room: Room,
        table: Table,
    ): Int? {
        val owners = table.deal.chestIds.mapNotNull { chests.inRoom(room, it)?.first?.ownerId }
        return when {
            !host.mayTrade(table.playerId) -> FAIL_CANT_TRADE
            owners.any { !host.mayTrade(it) } -> FAIL_OWNER_CANT_TRADE
            else -> null
        }
    }

    private fun settleDeposit(
        scope: RoomScope,
        table: Table,
        deal: Deal.Deposit,
    ) {
        val pair = chests.inRoom(scope.room, deal.chestId) ?: return cancel(scope, table, CANCEL_USER, REASON_USER)
        tablesOf(scope.room).remove(table.playerId)
        val player = table.playerId
        val set = moves.moves()
        val delta = set.of(pair)
        val chest = pair.second
        val offered = table.offered.values.toList()
        if (chest.isCoins) {
            moves.addCredits(delta, offered.sumOf { CreditFurni.credits(it.name) })
        } else {
            moves.store(delta, offered)
        }
        set.paid(player, offered.map { it.id })
        moves.record(scope, set, TransactionRow.MANUAL, "", player)
        host.refreshInventory(player)
        tellDonation(scope, pair, player)
        if (!chest.isCoins) {
            scope.out.send(
                Lane.Now,
                Audience.Player(player),
                ItemsChestContentsUpdatedOutCodec,
                TableWire.contentsUpdated(chest, emptyList(), delta.deposited),
            )
        }
        succeed(scope, player, TableWire.SUCCESS_DEPOSIT)
        moves.overflow(scope, set, player)
        if (!chest.isCoins && pair.first.ownerId == player) {
            val inUse =
                chests.all(scope.room).count { (f, c) ->
                    f.ownerId == player && !c.isCoins &&
                        c.furni.isNotEmpty()
                }
            host.furniChestsInUse(player, inUse)
        }
    }

    private fun tellDonation(
        scope: RoomScope,
        pair: Pair<RoomFurni, Chest>,
        player: Int,
    ) {
        val (furni, chest) = pair
        val name = host.name(player)
        notices.tell(scope, furni.ownerId, chest, ChestNotice.DONATION, player, name)
        if (chest.contents >= chest.capacity) notices.tell(scope, furni.ownerId, chest, ChestNotice.FULL, player, name)
    }

    private fun succeed(
        scope: RoomScope,
        player: Int,
        kind: Int,
    ) {
        val to = Audience.Player(player)
        scope.out.send(Lane.Now, to, WiredTransactionSuccessOutCodec, TableWire.success(kind))
        scope.out.send(Lane.Now, to, WiredTradeCompletedOutCodec, WiredTradeCompletedOut)
    }

    private fun settleDeal(
        scope: RoomScope,
        table: Table,
        deal: Deal.Negotiated,
    ) {
        val plan = DealPlan.of(table, deal, chests.wiredOf(scope.room, deal.chestIds))
        if (plan == null) {
            cancel(scope, table, CANCEL_NO_COVER, FAIL_FUNDS)
            notices.failureToRoom(scope)
            return
        }
        tablesOf(scope.room).remove(table.playerId)
        val player = table.playerId
        host.putBack(player, plan.leftOver.map { it.id })
        val set = moves.moves()
        set.paid(player, plan.paid.map { it.id })
        applyPlan(set, plan)
        set.deliver(player, plan.furniOut.flatMap { it.second })
        set.payout(player, plan.creditsOut, WiredCreditEarnings.PURSE)
        moves.record(scope, set, DealPlan.rowType(deal.contract), DealPlan.definition(deal, plan.sets), player)
        host.refreshInventory(player)
        succeed(
            scope,
            player,
            if (deal.contract.kind ==
                ContractKind.TRADE
            ) {
                TableWire.SUCCESS_TRADE
            } else {
                TableWire.SUCCESS_PAYMENT
            },
        )
        moves.tellOwners(scope, set, player)
        moves.overflow(scope, set, player)
        coinAchievements(plan, player)
        val moved =
            TransactionTriggers.Completed(
                plan.sets,
                plan.paidFurni.size,
                plan.credits,
                plan.furniOut.sumOf { it.second.size },
                plan.creditsOut,
            )
        triggers.completed(
            scope,
            deal.contractId,
            scope.room.units
                .ofPlayer(player)
                ?.index,
            moved,
        )
    }

    private fun applyPlan(
        set: ChestMoves.Moves,
        plan: DealPlan,
    ) {
        if (plan.credits > 0) plan.coinChest?.let { moves.addCredits(set.of(it), plan.credits) }
        if (plan.paidFurni.isNotEmpty()) plan.furniChest?.let { moves.store(set.of(it), plan.paidFurni) }
        plan.furniOut.forEach { (pair, furni) -> moves.takeFurni(set.of(pair), furni) }
        plan.coinsOut.forEach { (pair, credits) -> moves.takeCredits(set.of(pair), credits) }
    }

    private fun coinAchievements(
        plan: DealPlan,
        player: Int,
    ) {
        if (plan.credits <= 0) return
        host.progress(player, GameEvents.TRADE_COIN_VALUE, 1)
        plan.coinChest?.let { host.progress(it.first.ownerId, GameEvents.CHEST_COIN_SALE, 1) }
    }

    fun fail(
        scope: RoomScope,
        playerId: Int,
        reason: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        WiredTransactionFailOutCodec,
        WiredTransactionFailOut(reason),
    )

    private fun tradeError(
        scope: RoomScope,
        playerId: Int,
        error: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        WiredTradeTransactionNotificationOutCodec,
        WiredTradeTransactionNotificationOut(error),
    )

    companion object {
        /** A table closes about 3 s after its time-out's seconds. */
        const val TIMEOUT_GRACE = 3000L
        const val FAIL_NEGOTIATING = 4
        const val FAIL_MISCONFIGURED = 5
        const val FAIL_FUNDS = 6
        const val FAIL_CANT_TRADE = 8
        const val FAIL_OWNER_CANT_TRADE = 9
        const val FAIL_EMPTY = 10
        const val FAIL_NO_CHESTS = 15
        const val FAIL_CAPACITY = 19
        const val CANCEL_USER = 0
        const val CANCEL_UNMET = 1
        const val CANCEL_TIMEOUT = 2
        const val CANCEL_BY_WIRED = 3
        const val CANCEL_NO_COVER = 6
        const val REASON_USER = 0
        const val REASON_INVALID = 1
        const val REASON_LEFT = 25
        const val ERROR_INVALID = 0
        const val ERROR_ENOUGH = 4
        const val ERROR_CAPACITY = 9
    }
}
