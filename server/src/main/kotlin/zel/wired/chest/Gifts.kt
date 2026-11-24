package zel.wired.chest

import zel.core.rules.rules
import zel.game.achievements.GameEvents
import zel.game.earnings.WiredCreditEarnings
import zel.protocol.generated.WiredTransactionSuccessOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class Gifts(
    private val moves: ChestMoves,
    private val host: ChestHost,
    private val deals: ChestDeals,
    private val notices: ChestNotices,
    private val random: Random = Random.Default,
    private val furniGapMillis: () -> Long = { rules.rates.wiredGiveFurniGapMillis },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val lastFurniGift = ConcurrentHashMap<Int, Long>()

    data class Gift(
        val playerId: Int,
        val boxId: Int,
        val amount: Int,
        val giveAll: Boolean,
        val popupText: String,
        val popup: Boolean,
        val roomOwner: Int,
    )

    fun furni(
        scope: RoomScope,
        gift: Gift,
        chests: List<Pair<RoomFurni, Chest>>,
        iteration: Int,
    ) {
        if (!mayReceive(scope, gift.playerId, chests, chests.sumOf { it.second.furni.size })) return
        if (!furniGapPassed(gift.playerId)) return
        val set = moves.moves()
        var left = if (gift.giveAll) Int.MAX_VALUE else gift.amount
        val given = ArrayList<StoredFurni>()
        chests.forEach { pair ->
            val found = ordered(pair.second.furni, iteration).take(left)
            if (found.isNotEmpty()) moves.takeFurni(set.of(pair), found)
            given += found
            left -= found.size
        }
        val iterationName = ITERATIONS.getOrElse(iteration) { ITERATIONS[0] }
        val definition = "wired id: ${gift.boxId}, amount: ${given.size}, iteration type: $iterationName"
        set.deliver(gift.playerId, given)
        moves.finish(scope, set, TransactionRow.WIRED, definition, gift.playerId)
        val terms =
            given
                .groupBy { it.isWall to it.spriteId }
                .map { (key, furni) -> Term(false, furni.size, key.first, key.second) }
        success(scope, gift, terms)
        moves.tellOwners(scope, set, gift.playerId)
    }

    /** Games pay counts in someone else's room. */
    fun credits(
        scope: RoomScope,
        gift: Gift,
        chests: List<Pair<RoomFurni, Chest>>,
        category: Int,
    ) {
        if (!mayReceive(scope, gift.playerId, chests, chests.sumOf { it.second.credits })) return
        val set = moves.moves()
        var left = if (gift.giveAll) Int.MAX_VALUE else gift.amount
        var given = 0
        chests.forEach { pair ->
            val part = minOf(left, pair.second.credits)
            if (part > 0) moves.takeCredits(set.of(pair), part)
            given += part
            left -= part
        }
        if (given <= 0) return
        val earned = WiredCreditEarnings.categoryOf(category)
        val toEarnings = earned != WiredCreditEarnings.PURSE
        set.payout(gift.playerId, given, earned, announced = toEarnings)
        moves.finish(scope, set, TransactionRow.WIRED, "wired id: ${gift.boxId}, amount: $given", gift.playerId)
        if (toEarnings) host.announceEarning(gift.playerId, earned)
        success(scope, gift, listOf(Term(true, given)))
        moves.tellOwners(scope, set, gift.playerId)
        if (category == Contract.CATEGORY_GAMES && gift.playerId != gift.roomOwner) {
            host.progress(gift.playerId, GameEvents.CHEST_GIFT_GIVEN, given)
        }
    }

    /** A sooner firing is dropped unanswered; an empty chest answers every one. */
    private fun furniGapPassed(playerId: Int): Boolean {
        val now = clock()
        val gap = furniGapMillis()
        lastFurniGift.values.removeIf { now - it >= gap }
        return lastFurniGift.putIfAbsent(playerId, now) == null
    }

    /** A prize is a trade from the chest owners to the receiver: both sides must be free to trade. */
    private fun mayReceive(
        scope: RoomScope,
        playerId: Int,
        chests: List<Pair<RoomFurni, Chest>>,
        held: Int,
    ): Boolean {
        val barred =
            when {
                !host.mayTrade(playerId) -> ChestDeals.FAIL_CANT_TRADE
                chests.any { !host.mayTrade(it.first.ownerId) } -> ChestDeals.FAIL_OWNER_CANT_TRADE
                else -> null
            }
        if (barred != null) {
            deals.fail(scope, playerId, barred)
            return false
        }
        if (held > 0) return true
        deals.fail(scope, playerId, ChestDeals.FAIL_FUNDS)
        notices.failureToRoom(scope)
        return false
    }

    private fun ordered(
        furni: List<StoredFurni>,
        iteration: Int,
    ): List<StoredFurni> =
        when (iteration) {
            FIRST_IN -> furni
            LAST_IN -> furni.reversed()
            else -> furni.shuffled(random)
        }

    private fun success(
        scope: RoomScope,
        gift: Gift,
        rewards: List<Term>,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(gift.playerId),
        WiredTransactionSuccessOutCodec,
        TableWire.rewardSuccess(rewards, gift.popupText, gift.popup),
    )

    private companion object {
        const val FIRST_IN = 1
        const val LAST_IN = 2
        val ITERATIONS = listOf("RANDOM", "FIRST_IN_FIRST_OUT", "LAST_IN_FIRST_OUT")
    }
}
