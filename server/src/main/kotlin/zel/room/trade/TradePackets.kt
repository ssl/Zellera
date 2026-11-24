package zel.room.trade

import zel.core.rules.rules
import zel.protocol.generated.AcceptTradingInCodec
import zel.protocol.generated.AddItemToTradeInCodec
import zel.protocol.generated.AddItemsToTradeInCodec
import zel.protocol.generated.CloseTradingInCodec
import zel.protocol.generated.ConfirmAcceptTradingInCodec
import zel.protocol.generated.ConfirmDeclineTradingInCodec
import zel.protocol.generated.OpenTradingInCodec
import zel.protocol.generated.RemoveItemFromTradeInCodec
import zel.protocol.generated.UnacceptTradingInCodec
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.protocol.trading.TradeMessages
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.EntryStore
import zel.room.entry.RoomLeaving
import kotlin.math.abs

@Suppress("LongParameterList") // room access plus the trade parts
class TradePackets(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val trades: RoomTrades,
    private val messages: TradeMessages,
    private val entries: EntryStore,
    private val requests: TradeRequests,
) : PacketPackage {
    override val name = "trades"
    override val routes =
        listOf(
            PacketRoute(OpenTradingInCodec) { session, m -> requestTrade(session, m.userId) },
            PacketRoute(AddItemToTradeInCodec) { session, m ->
                withOwnSide(session) { _, trade, side ->
                    OfferedFurni.one(m.itemId, stash(side))?.let { trades.offer(trade, side, listOf(it)) }
                }
            },
            PacketRoute(AddItemsToTradeInCodec) { session, m ->
                withOwnSide(session) { _, trade, side ->
                    OfferedFurni
                        .batch(m.itemIds, rules.tradeBatchCap, stash(side))
                        ?.let { trades.offer(trade, side, it) }
                }
            },
            PacketRoute(RemoveItemFromTradeInCodec) { session, m ->
                withOwnSide(session) { _, trade, side ->
                    if (side.accepted) return@withOwnSide
                    side.items.find { it.id == abs(m.itemId) }?.let { trades.takeBack(trade, side, it) }
                }
            },
            PacketRoute(AcceptTradingInCodec) { session, _ ->
                withOwnSide(session) { _, trade, side -> trades.accept(trade, side, true) }
            },
            PacketRoute(UnacceptTradingInCodec) { session, _ ->
                withOwnSide(session) { _, trade, side -> trades.accept(trade, side, false) }
            },
            PacketRoute(ConfirmAcceptTradingInCodec) { session, _ ->
                withOwnSide(session) { scope, trade, side -> trades.confirm(scope, trade, side) }
            },
            PacketRoute(CloseTradingInCodec) { session, _ -> cancel(session) },
            PacketRoute(ConfirmDeclineTradingInCodec) { session, _ -> cancel(session) },
        )

    private fun stash(side: Trade.Side) = side.player.inventory.furni::get

    private fun cancel(session: PacketSession) =
        withOwnSide(session) { scope, trade, side -> trades.close(scope, trade, side.id) }

    private fun requestTrade(
        session: PacketSession,
        partnerIndex: Int,
    ) {
        val asker = session.client.player ?: return
        val roomId = registry.roomOf(asker.profile.id) ?: return
        val tradeMode = entries.card(roomId)?.tradeMode ?: return
        registry.withRoom(roomId) { scope ->
            val askerIndex = leaving.doorOf(scope.room).present[asker.profile.id]?.unitIndex ?: return@withRoom
            answer(session, scope, requests.judge(scope, asker, askerIndex, partnerIndex, tradeMode))
        }
    }

    private fun answer(
        session: PacketSession,
        scope: RoomScope,
        opening: TradeOpening,
    ) {
        when (opening) {
            TradeOpening.Ignored -> Unit
            TradeOpening.NeedsTradePass -> session.client.send(BubbleAlertMessage(TRADE_PASS_DIALOG))
            is TradeOpening.Refused -> messages.startFailed(session.client, opening.failure, opening.partnerName)
            is TradeOpening.Opens -> trades.start(scope, opening.asker, opening.partner)
        }
    }

    private fun withOwnSide(
        session: PacketSession,
        action: (RoomScope, Trade, Trade.Side) -> Unit,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        val roomId = registry.roomOf(playerId) ?: return
        registry.withRoom(roomId) { scope ->
            val trade = trades.tradeOf(scope.room, playerId) ?: return@withRoom
            trade.sideOf(playerId)?.let { action(scope, trade, it) }
        }
    }

    private companion object {
        const val TRADE_PASS_DIALOG = "trade.trading_perk"
    }
}
