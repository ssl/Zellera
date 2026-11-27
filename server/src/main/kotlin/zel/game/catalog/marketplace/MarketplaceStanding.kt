package zel.game.catalog.marketplace

import zel.game.players.Player
import zel.protocol.outgoing.catalog.MarketplaceSellStatus

/**
 * Selling and buying here hand furni from one player to another, so the marketplace asks what a room trade asks:
 * the trade pass, and no trade lock. The answer is the sell status the client shows for it.
 */
internal object MarketplaceStanding {
    fun refusal(player: Player): MarketplaceSellStatus? {
        val standing = player.stats.standing
        return when {
            !standing.tradePass -> MarketplaceSellStatus.NO_TRADE_PASS
            !standing.mayTrade() -> MarketplaceSellStatus.CLOSED
            !standing.safetyAllows() -> MarketplaceSellStatus.CLOSED
            else -> null
        }
    }
}
