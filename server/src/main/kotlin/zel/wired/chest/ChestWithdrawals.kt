package zel.wired.chest

import zel.game.earnings.WiredCreditEarnings
import zel.protocol.generated.CoinsChestContentsOut
import zel.protocol.generated.CoinsChestContentsOutCodec
import zel.protocol.generated.ItemsChestContentsUpdatedOutCodec
import zel.protocol.generated.WiredTransactionSuccessOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.form.WiredMasks

/** Checks wired modify rights, the TRADE perk, the lock, then the contents. */
class ChestWithdrawals(
    private val chests: RoomChests,
    private val deals: ChestDeals,
    private val moves: ChestMoves,
    private val notices: ChestNotices,
    private val host: ChestHost,
    private val masks: (RoomScope) -> WiredMasks,
) {
    fun items(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        request: ItemsWithdrawal,
    ) = checked(scope, playerId, level, request.chestId) { pair ->
        val found =
            pair.second.furni
                .filter { it.isWall == request.wall && it.spriteId == request.typeId }
                .take(request.quantity.coerceAtLeast(0))
        furni(scope, playerId, pair, found)
    }

    fun coins(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        chestId: Int,
        amount: Int,
    ) = checked(scope, playerId, level, chestId) { pair ->
        if (pair.second.isCoins &&
            amount in 1..pair.second.credits
        ) {
            credits(scope, playerId, pair, amount)
        } else {
            empty(scope, playerId)
        }
    }

    fun all(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        chestId: Int,
    ) = checked(scope, playerId, level, chestId) { pair ->
        val chest = pair.second
        when {
            chest.contents == 0 -> empty(scope, playerId)
            chest.isCoins -> credits(scope, playerId, pair, chest.credits)
            else -> furni(scope, playerId, pair, chest.furni.toList())
        }
    }

    private fun checked(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        chestId: Int,
        withdraw: (Pair<RoomFurni, Chest>) -> Unit,
    ) {
        val pair = chests.inRoom(scope.room, chestId) ?: return
        val (furni, chest) = pair
        // A withdrawal by someone without wired modify rights gets no answer at all.
        when {
            !masks(scope).mayModify(level) -> Unit
            !host.mayTrade(playerId) -> deals.fail(scope, playerId, ChestDeals.FAIL_CANT_TRADE)
            !host.mayTrade(furni.ownerId) -> deals.fail(scope, playerId, ChestDeals.FAIL_OWNER_CANT_TRADE)
            chest.locked && furni.ownerId != playerId -> Unit
            else -> withdraw(pair)
        }
    }

    private fun empty(
        scope: RoomScope,
        playerId: Int,
    ) {
        deals.fail(scope, playerId, ChestDeals.FAIL_EMPTY)
        notices.failureToRoom(scope)
    }

    private fun furni(
        scope: RoomScope,
        playerId: Int,
        pair: Pair<RoomFurni, Chest>,
        found: List<StoredFurni>,
    ) {
        if (found.isEmpty()) return empty(scope, playerId)
        val set = moves.moves()
        moves.takeFurni(set.of(pair), found)
        set.deliver(playerId, found)
        moves.finish(scope, set, TransactionRow.MANUAL, "", playerId)
        success(scope, playerId)
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            ItemsChestContentsUpdatedOutCodec,
            TableWire.contentsUpdated(pair.second, found.map { it.slotId }, emptyList()),
        )
        tell(scope, playerId, pair)
    }

    private fun credits(
        scope: RoomScope,
        playerId: Int,
        pair: Pair<RoomFurni, Chest>,
        amount: Int,
    ) {
        val set = moves.moves()
        moves.takeCredits(set.of(pair), amount)
        set.payout(playerId, amount, WiredCreditEarnings.PURSE)
        moves.finish(scope, set, TransactionRow.MANUAL, "", playerId)
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            CoinsChestContentsOutCodec,
            CoinsChestContentsOut(pair.second.id, pair.second.credits, true),
        )
        success(scope, playerId)
        tell(scope, playerId, pair)
    }

    private fun success(
        scope: RoomScope,
        playerId: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        WiredTransactionSuccessOutCodec,
        TableWire.success(TableWire.SUCCESS_WITHDRAW),
    )

    private fun tell(
        scope: RoomScope,
        playerId: Int,
        pair: Pair<RoomFurni, Chest>,
    ) = notices.tell(scope, pair.first.ownerId, pair.second, ChestNotice.WITHDRAW, playerId, host.name(playerId))
}
