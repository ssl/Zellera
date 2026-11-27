package zel.wired.chest

import zel.game.earnings.WiredCreditEarnings
import zel.game.economy.LedgerEntry
import zel.room.RoomScope
import zel.room.furni.RoomFurni

class ChestMoves(
    private val chests: RoomChests,
    private val log: ChestLog,
    private val notices: ChestNotices,
    private val host: ChestHost,
) {
    inner class Moves {
        private val deltas = LinkedHashMap<Int, ChestDelta>()
        private val paid = ArrayList<Int>()
        private var payerId = 0
        private val delivered = ArrayList<ChestSettlement.Delivery>()
        private val payouts = ArrayList<ChestSettlement.Payout>()
        internal var rows: List<TransactionRow> = emptyList()

        fun of(pair: Pair<RoomFurni, Chest>): ChestDelta =
            deltas.getOrPut(pair.second.id) { ChestDelta(pair.first, pair.second, log.nextId()) }

        fun all(): List<ChestDelta> = deltas.values.toList()

        fun paid(
            playerId: Int,
            itemIds: List<Int>,
        ) {
            payerId = playerId
            paid += itemIds
        }

        fun deliver(
            playerId: Int,
            furni: List<StoredFurni>,
        ) {
            if (furni.isNotEmpty()) delivered += ChestSettlement.Delivery(playerId, furni)
        }

        /** [announced]: the receiver was already told of the earning, ahead of the answer that follows it. */
        fun payout(
            playerId: Int,
            credits: Int,
            category: Int,
            announced: Boolean = false,
        ) {
            if (credits <= 0) return
            val operation = LedgerEntry.newKey("chest-credits:$playerId")
            payouts += ChestSettlement.Payout(playerId, credits, category, operation, log.seconds(), announced)
        }

        internal fun settlement(): ChestSettlement =
            ChestSettlement(
                paid.toList(),
                deltas.values.associate { it.chest.id to ChestRecord.of(it.chest) },
                rows,
                delivered.toList(),
                payouts.toList(),
                payerId,
            )

        internal fun reversal(): List<Reversal> =
            deltas.values.map {
                Reversal(it.chest.id, it.deposited.toList(), it.withdrawn.toList(), it.creditsIn, it.creditsOut)
            }
    }

    class Reversal(
        val chestId: Int,
        val deposited: List<StoredFurni>,
        val withdrawn: List<StoredFurni>,
        val creditsIn: Int,
        val creditsOut: Int,
    )

    fun moves(): Moves = Moves()

    fun store(
        delta: ChestDelta,
        offered: List<OfferedFurni>,
    ) {
        offered.forEach { item ->
            val stored =
                StoredFurni(
                    slotId = item.id,
                    transactionId = delta.transactionId,
                    isWall = item.wall,
                    definitionId = item.definitionId,
                    spriteId = item.spriteId,
                    posterCode = "",
                    groupable = !item.wall,
                    state = item.state,
                    limitedNumber = item.limitedNumber,
                    limitedTotal = item.limitedTotal,
                    extra = item.extra,
                    furniId = item.id,
                )
            delta.chest.furni += stored
            delta.deposited += stored
        }
    }

    fun addCredits(
        delta: ChestDelta,
        credits: Int,
    ) {
        delta.chest.credits += credits
        delta.creditsIn += credits
    }

    fun takeCredits(
        delta: ChestDelta,
        credits: Int,
    ) {
        delta.chest.credits -= credits
        delta.creditsOut += credits
    }

    fun takeFurni(
        delta: ChestDelta,
        furni: List<StoredFurni>,
    ) {
        delta.chest.furni.removeAll(furni.toSet())
        delta.withdrawn += furni
    }

    fun finish(
        scope: RoomScope,
        moves: Moves,
        type: Int,
        definition: String,
        actorId: Int,
    ) {
        record(scope, moves, type, definition, actorId)
        settle(scope, listOf(moves))
    }

    fun record(
        scope: RoomScope,
        moves: Moves,
        type: Int,
        definition: String,
        actorId: Int,
    ) {
        moves.rows = moves.all().map { delta -> row(scope, delta, type, definition, actorId) }
        moves.all().forEach { delta -> chests.shown(scope, delta.furni, delta.chest) }
    }

    /** A failed write takes every move back, the last first. */
    private fun settle(
        scope: RoomScope,
        all: List<Moves>,
    ) {
        val settlement = all.map { it.settlement() }.reduce(ChestSettlement::plus)
        val reversals = all.flatMap { it.reversal() }.reversed()
        host.settle(scope.room.info.id, settlement) { inner -> reverse(inner, reversals) }
    }

    /** Saves the reverted state, so no later save writes lost state. */
    private fun reverse(
        scope: RoomScope,
        reversals: List<Reversal>,
    ) {
        reversals.forEach { back ->
            val (_, chest) = chests.inRoom(scope.room, back.chestId) ?: return@forEach
            chest.furni.removeAll(back.deposited.toSet())
            chest.furni += back.withdrawn.filterNot { it in chest.furni }
            chest.credits = (chest.credits - back.creditsIn + back.creditsOut).coerceAtLeast(0)
        }
        reversals.map { it.chestId }.distinct().forEach { id ->
            chests.inRoom(scope.room, id)?.let { (furni, chest) -> chests.changed(scope, furni, chest) }
        }
    }

    private fun row(
        scope: RoomScope,
        delta: ChestDelta,
        type: Int,
        definition: String,
        actorId: Int,
    ): TransactionRow =
        TransactionRow(
            id = delta.transactionId,
            roomId = scope.room.info.id,
            chestId = delta.chest.id,
            coins = delta.chest.isCoins,
            type = type,
            definition = definition,
            userId = actorId,
            userName = host.name(actorId),
            withdrawn = if (delta.chest.isCoins) delta.creditsOut else delta.withdrawn.size,
            deposited = if (delta.chest.isCoins) delta.creditsIn else delta.deposited.size,
            timestamp = log.seconds(),
            furniIn = if (delta.chest.isCoins) emptyList() else Details.lines(delta.deposited),
            furniOut = if (delta.chest.isCoins) emptyList() else Details.lines(delta.withdrawn),
        )

    fun tellOwners(
        scope: RoomScope,
        moves: Moves,
        actorId: Int,
    ) {
        val name = host.name(actorId)
        moves.all().forEach { delta ->
            val owner = delta.furni.ownerId
            val took = delta.creditsOut > 0 || delta.withdrawn.isNotEmpty()
            if (took && delta.chest.contents == 0) {
                notices.tell(scope, owner, delta.chest, ChestNotice.EMPTY, actorId, name)
            }
            notices.tell(scope, owner, delta.chest, ChestNotice.WIRED, actorId, name)
        }
    }

    fun overflow(
        scope: RoomScope,
        moves: Moves,
        actorId: Int,
    ) {
        val handed =
            moves.all().mapNotNull { delta ->
                val chest = delta.chest
                val over = (chest.contents - chest.capacity).coerceAtLeast(0)
                if (over > 0) handOver(scope, delta, over, actorId) else null
            }
        settle(scope, listOf(moves) + handed.map { it.first })
        handed.forEach { (_, delta) -> notices.earnings(scope, delta.furni.ownerId, delta.chest) }
    }

    private fun handOver(
        scope: RoomScope,
        delta: ChestDelta,
        over: Int,
        actorId: Int,
    ): Pair<Moves, ChestDelta> {
        val chest = delta.chest
        val owner = delta.furni.ownerId
        val moved = moves()
        val auto = moved.of(delta.furni to chest)
        if (chest.isCoins) {
            takeCredits(auto, over)
            moved.payout(owner, over, WiredCreditEarnings.CHEST_OVERFLOW)
        } else {
            val paid = delta.deposited.takeLast(over)
            val older = chest.furni.filterNot { it in paid }.take(over - paid.size)
            takeFurni(auto, paid + older)
            moved.deliver(owner, paid + older)
        }
        record(scope, moved, TransactionRow.AUTO_WITHDRAW, CAPACITY_EXCEEDED, actorId)
        return moved to delta
    }

    private companion object {
        const val CAPACITY_EXCEEDED = "Capacity exceeded"
    }
}
