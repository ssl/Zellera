package zel.protocol.trading

import zel.game.items.OwnedFurni
import zel.net.clients.ClientConnection

interface TradeMessages {
    fun open(
        client: ClientConnection,
        firstUserId: Int,
        secondUserId: Int,
    )

    fun accepted(
        client: ClientConnection,
        userId: Int,
        accepted: Boolean,
    )

    fun waitingConfirm(client: ClientConnection)

    fun completed(client: ClientConnection)

    fun closed(
        client: ClientConnection,
        userId: Int,
        reason: Int,
    )

    fun offers(
        client: ClientConnection,
        offers: List<Pair<Int, List<OwnedFurni>>>,
    )

    fun startFailed(
        client: ClientConnection,
        failure: Int,
        username: String = "",
    )
}
