package zel.game.catalog.marketplace

import zel.game.economy.BalanceChange
import zel.game.economy.EconomyLedger
import zel.game.players.Player
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.outgoing.users.UserCredits

/** [setting] is a ledger code: [EconomyLedger.CREDITS] for credits, else the activity-point type. */
internal data class MarketplaceCurrency(
    private val setting: Int,
) {
    val isCredits: Boolean get() = setting == EconomyLedger.CREDITS

    val ledgerType: Int get() = setting

    fun balanceOf(player: Player): Int = if (isCredits) player.profile.credits else player.profile.balance(setting)

    fun canPay(
        player: Player,
        price: Int,
    ): Boolean = price <= balanceOf(player)

    fun tell(
        player: Player,
        change: BalanceChange,
    ) {
        val client = player.client ?: return
        if (isCredits) {
            client.send(UserCredits(player))
        } else {
            client.send(
                ActivityPointNotificationOutCodec,
                ActivityPointNotificationOut(
                    change.after,
                    change.after - change.before,
                    setting,
                ),
            )
        }
    }
}
