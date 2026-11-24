package zel.game.catalog

import zel.game.economy.EconomyLedger
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogReplies

data class BalanceShortfall(
    val credits: Boolean,
    val points: Boolean,
    val pointsType: Int,
) {
    fun send(client: ClientConnection) = CatalogReplies.notEnoughBalance(client, credits, points, pointsType)

    companion object {
        fun of(
            credits: Int,
            points: Int,
            pointsType: Int,
            creditBalance: Int,
            pointsBalance: Int,
        ): BalanceShortfall? =
            when {
                credits > creditBalance -> {
                    BalanceShortfall(
                        credits = true,
                        points = false,
                        pointsType = EconomyLedger.POINTS,
                    )
                }

                points > pointsBalance -> {
                    BalanceShortfall(credits = false, points = true, pointsType = pointsType)
                }

                else -> {
                    null
                }
            }
    }
}
