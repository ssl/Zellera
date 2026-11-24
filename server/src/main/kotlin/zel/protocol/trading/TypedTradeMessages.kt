package zel.protocol.trading

import zel.game.items.OwnedFurni
import zel.net.clients.ClientConnection
import zel.protocol.generated.TradeOpenFailedOut
import zel.protocol.generated.TradeOpenFailedOutCodec
import zel.protocol.generated.TradingAcceptOut
import zel.protocol.generated.TradingAcceptOutCodec
import zel.protocol.generated.TradingCloseOut
import zel.protocol.generated.TradingCloseOutCodec
import zel.protocol.generated.TradingCompletedOut
import zel.protocol.generated.TradingCompletedOutCodec
import zel.protocol.generated.TradingConfirmationOut
import zel.protocol.generated.TradingConfirmationOutCodec
import zel.protocol.generated.TradingOpenOut
import zel.protocol.generated.TradingOpenOutCodec

class TypedTradeMessages : TradeMessages {
    override fun open(
        client: ClientConnection,
        firstUserId: Int,
        secondUserId: Int,
    ) = client.send(TradingOpenOutCodec, TradingOpenOut(firstUserId, OPEN_STATE, secondUserId, OPEN_STATE))

    override fun accepted(
        client: ClientConnection,
        userId: Int,
        accepted: Boolean,
    ) = client.send(TradingAcceptOutCodec, TradingAcceptOut(userId, if (accepted) 1 else 0))

    override fun waitingConfirm(client: ClientConnection) =
        client.send(TradingConfirmationOutCodec, TradingConfirmationOut)

    override fun completed(client: ClientConnection) = client.send(TradingCompletedOutCodec, TradingCompletedOut)

    override fun closed(
        client: ClientConnection,
        userId: Int,
        reason: Int,
    ) = client.send(TradingCloseOutCodec, TradingCloseOut(userId, reason))

    override fun offers(
        client: ClientConnection,
        offers: List<Pair<Int, List<OwnedFurni>>>,
    ) = client.send(TradeItemsLayout.codec, TradeItemsLayout.offers(offers))

    override fun startFailed(
        client: ClientConnection,
        failure: Int,
        username: String,
    ) = client.send(TradeOpenFailedOutCodec, TradeOpenFailedOut(failure, username))

    private companion object {
        const val OPEN_STATE = 1
    }
}
