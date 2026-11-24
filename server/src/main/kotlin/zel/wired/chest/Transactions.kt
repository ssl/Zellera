package zel.wired.chest

import zel.game.earnings.WiredCreditEarnings
import zel.protocol.generated.WiredTradeCancelledOut
import zel.protocol.generated.WiredTradeCancelledOutCodec
import zel.protocol.generated.WiredTradeInitiateOutCodec
import zel.protocol.generated.WiredTransactionSuccessOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane

class Transactions(
    private val chests: RoomChests,
    private val deals: ChestDeals,
    private val moves: ChestMoves,
    private val notices: ChestNotices,
    private val host: ChestHost,
    private val triggers: TransactionTriggers,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun open(
        scope: RoomScope,
        request: TransactionRequest,
    ) {
        val wired = chests.wiredOf(scope.room, request.chestIds)
        when {
            !host.mayTrade(request.playerId) -> {
                refuse(scope, request, ChestDeals.FAIL_CANT_TRADE)
            }

            wired.any { !host.mayTrade(it.first.ownerId) } -> {
                refuse(scope, request, ChestDeals.FAIL_OWNER_CANT_TRADE)
            }

            deals.table(scope.room, request.playerId) != null -> {
                refuse(scope, request, ChestDeals.FAIL_NEGOTIATING)
            }

            request.contract.paysAtOnce -> {
                payAtOnce(scope, request, wired)
            }

            else -> {
                negotiate(scope, request, wired)
            }
        }
    }

    private fun refuse(
        scope: RoomScope,
        request: TransactionRequest,
        reason: Int,
        toRoom: Boolean = false,
        told: Boolean = true,
        firesStack: Boolean = true,
    ) {
        if (told) deals.fail(scope, request.playerId, reason)
        if (toRoom) notices.failureToRoom(scope)
        if (firesStack) triggers.failed(scope, request.contractId, request.unitIndex, reason)
    }

    private fun payAtOnce(
        scope: RoomScope,
        request: TransactionRequest,
        wired: List<Pair<RoomFurni, Chest>>,
    ) {
        if (wired.isEmpty()) {
            // A reward with no chest picked shows the room's failure dialog only.
            refuse(scope, request, ChestDeals.FAIL_NO_CHESTS, toRoom = true, told = false, firesStack = false)
            return
        }
        if (request.contract.get.isEmpty()) return
        val contract = request.contract
        val sets =
            DealPlan.lowered(
                if (request.mode ==
                    Requirements.MODE_FIXED
                ) {
                    request.multiplier
                } else {
                    1
                },
                contract.get,
            )
        val out = DealPlan.rewards(contract.get, sets, wired)
        if (out == null) {
            refuse(scope, request, ChestDeals.FAIL_FUNDS, toRoom = true)
            return
        }
        val set = moves.moves()
        out.first.forEach { (pair, furni) -> moves.takeFurni(set.of(pair), furni) }
        out.second.forEach { (pair, credits) -> moves.takeCredits(set.of(pair), credits) }
        val credits = out.second.sumOf { it.second }
        val earned = WiredCreditEarnings.categoryOf(contract.reward.category)
        val toEarnings = credits > 0 && earned != WiredCreditEarnings.PURSE
        set.payout(request.playerId, credits, earned, announced = toEarnings)
        val deal = dealOf(request, emptyList())
        set.deliver(request.playerId, out.first.flatMap { it.second })
        moves.finish(scope, set, TransactionRow.REWARD, DealPlan.definition(deal, sets), request.playerId)
        val rewards = contract.get.map { it.copy(amount = it.amount * sets) }
        if (toEarnings) host.announceEarning(request.playerId, earned)
        scope.out.send(
            Lane.Now,
            Audience.Player(request.playerId),
            WiredTransactionSuccessOutCodec,
            TableWire.rewardSuccess(rewards, contract.reward.text, contract.reward.popup),
        )
        moves.tellOwners(scope, set, request.playerId)
        val moved =
            TransactionTriggers.Completed(sets, 0, 0, out.first.sumOf { it.second.size }, credits)
        triggers.completed(scope, request.contractId, request.unitIndex, moved)
    }

    private fun negotiate(
        scope: RoomScope,
        request: TransactionRequest,
        wired: List<Pair<RoomFurni, Chest>>,
    ) {
        val contract = request.contract
        val isTrade = contract.get.isNotEmpty()
        when {
            contract.isUnreadable && contract.give.isEmpty() -> {
                refuse(scope, request, ChestDeals.FAIL_MISCONFIGURED, toRoom = true, told = false, firesStack = false)
            }

            wired.isEmpty() && !isTrade -> {
                refuse(scope, request, ChestDeals.FAIL_NO_CHESTS, toRoom = true, told = false, firesStack = false)
            }

            !kindsPicked(contract, wired) -> {
                refuse(scope, request, ChestDeals.FAIL_NO_CHESTS, toRoom = true)
            }

            else -> {
                openTable(scope, request, wired)
            }
        }
    }

    private fun kindsPicked(
        contract: Contract,
        wired: List<Pair<RoomFurni, Chest>>,
    ): Boolean {
        if (wired.isEmpty()) return false
        if (contract.isDonation) return true
        val coins = wired.any { it.second.isCoins }
        val furni = wired.any { !it.second.isCoins }

        fun served(terms: List<Term>) = terms.all { if (it.credits) coins else furni }
        return contract.give.any(::served) && served(contract.get)
    }

    private fun openTable(
        scope: RoomScope,
        request: TransactionRequest,
        wired: List<Pair<RoomFurni, Chest>>,
    ) {
        val contract = request.contract
        val shown = if (request.mode == Requirements.MODE_FIXED) request.multiplier else 1
        val out = DealPlan.rewards(contract.get, shown, wired)
        val side = out?.first?.flatMap { it.second }.orEmpty()
        val deal = dealOf(request, side)
        val table = Table(request.playerId, deal, clock())
        val initiate =
            if (contract.isDonation) {
                TableWire.donationInitiate(deal, wired.all { it.second.isCoins })
            } else {
                TableWire.contractInitiate(deal)
            }
        if (out != null) {
            deals.open(scope, table, initiate)
            return
        }
        scope.out.send(
            Lane.Now,
            Audience.Player(request.playerId),
            WiredTradeInitiateOutCodec,
            initiate,
        )
        scope.out.send(
            Lane.Now,
            Audience.Player(request.playerId),
            WiredTradeCancelledOutCodec,
            WiredTradeCancelledOut(ChestDeals.CANCEL_NO_COVER),
        )
        notices.failureToRoom(scope)
        triggers.failed(scope, request.contractId, request.unitIndex, ChestDeals.FAIL_FUNDS)
    }

    private fun dealOf(
        request: TransactionRequest,
        side: List<StoredFurni>,
    ): Deal.Negotiated =
        Deal.Negotiated(
            request.contractId,
            request.contract,
            request.mode,
            request.multiplier,
            request.chestIds,
            side,
            request.timeoutSeconds,
        )
}
