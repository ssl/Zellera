package zel.room.trade

import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.protocol.trading.TradeStartFailure
import zel.room.RoomScope
import zel.room.unit.UnitKind

class TradeRequests(
    private val trades: RoomTrades,
    private val tradingEnabled: () -> Boolean,
    private val playerById: (Int) -> Player?,
    private val hasRights: (RoomScope, Int) -> Boolean,
) {
    @Suppress("ReturnCount") // one early answer per step
    fun judge(
        scope: RoomScope,
        asker: Player,
        askerIndex: Int,
        partnerIndex: Int,
        tradeMode: Int,
    ): TradeOpening {
        if (partnerIndex < 0 || partnerIndex == askerIndex) return TradeOpening.Ignored
        placeFailure(scope, asker, tradeMode)?.let { return TradeOpening.Refused(it) }
        val partner = playerAt(scope, partnerIndex) ?: return TradeOpening.Ignored
        if (partner.stats.contacts.userIgnored(asker.profile.id)) return TradeOpening.Ignored
        if (!asker.stats.standing.tradePass) return TradeOpening.NeedsTradePass
        return standing(scope, asker, Role.ASKER)
            ?: standing(scope, partner, Role.PARTNER)
            ?: TradeOpening.Opens(Trade.Side(asker, askerIndex), Trade.Side(partner, partnerIndex))
    }

    private fun placeFailure(
        scope: RoomScope,
        asker: Player,
        tradeMode: Int,
    ): Int? {
        if (asker.hasPermission(Privilege.ROOM_TRADE_ANYWHERE)) return null
        if (!tradingEnabled()) return TradeStartFailure.GAME_TRADING_NOT_ALLOWED
        val roomAllows =
            when (tradeMode) {
                NO_TRADING -> false
                OWNER_TRADES -> asker.profile.id == scope.room.info.ownerId || hasRights(scope, asker.profile.id)
                else -> true
            }
        return if (roomAllows) null else TradeStartFailure.ROOM_TRADING_NOT_ALLOWED
    }

    private fun playerAt(
        scope: RoomScope,
        index: Int,
    ): Player? = (scope.room.units[index]?.kind as? UnitKind.Player)?.let { playerById(it.playerId) }

    private fun standing(
        scope: RoomScope,
        player: Player,
        role: Role,
    ): TradeOpening.Refused? {
        val failure =
            when {
                trades.tradeOf(scope.room, player.profile.id) != null -> role.busy
                !player.stats.standing.mayTrade() -> role.barred
                !player.stats.standing.safetyAllows() -> role.barred
                else -> return null
            }
        return TradeOpening.Refused(failure, if (role.named) player.profile.name else "")
    }

    private enum class Role(
        val busy: Int,
        val barred: Int,
        val named: Boolean,
    ) {
        ASKER(TradeStartFailure.YOU_ALREADY_TRADING, TradeStartFailure.YOU_TRADE_LOCKED, named = false),
        PARTNER(TradeStartFailure.TARGET_ALREADY_TRADING, TradeStartFailure.TARGET_TRADING_NOT_ALLOWED, named = true),
    }

    private companion object {
        const val NO_TRADING = 0
        const val OWNER_TRADES = 1
    }
}
