package zel.game.players

import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.protocol.Outbound
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.outgoing.users.UserCredits
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PlayerWallet::class.java)

class PlayerWallet internal constructor(
    private val player: Player,
) {
    private val userId: Int get() = player.profile.id

    fun changeCredits(
        delta: Int,
        reason: String = UNNAMED_CAUSE,
    ) {
        if (delta == 0) return
        post(entry(reason, "credits:$userId", EconomyLedger.CREDITS, delta), logRefusal = true)
    }

    fun takeCredits(amount: Int): Boolean {
        if (amount <= 0) return false
        return post(entry(UNNAMED_CAUSE, "credits:$userId", EconomyLedger.CREDITS, -amount), false)
    }

    fun changePoints(
        type: Int,
        delta: Int,
        reason: String = UNNAMED_CAUSE,
    ) {
        if (delta == 0) return
        post(entry(reason, "points:$type:$userId", type, delta), logRefusal = true)
    }

    fun takePoints(
        type: Int,
        amount: Int,
        reason: String,
        operationId: String,
        actorId: Int?,
    ): Boolean {
        if (amount <= 0) return false
        val debit = LedgerEntry(operationId, userId, type, -amount, reason, actorId.takeIf { it != userId })
        return post(debit, logRefusal = false)
    }

    private fun entry(
        reason: String,
        namespace: String,
        currency: Int,
        delta: Int,
    ) = LedgerEntry(LedgerEntry.newKey(namespace), userId, currency, delta, reason)

    @Suppress("TooGenericExceptionCaught") // a refused or failed ledger post takes nothing; the game carries on
    private fun post(
        operation: LedgerEntry,
        logRefusal: Boolean,
    ): Boolean =
        try {
            val change = WalletWrites.post(player, operation)
            player.client?.send(
                if (operation.currency == EconomyLedger.CREDITS) {
                    UserCredits(player).compose()
                } else {
                    Outbound(
                        ActivityPointNotificationOutCodec,
                        ActivityPointNotificationOut(
                            change.after,
                            change.after - change.before,
                            operation.currency,
                        ),
                    )
                },
            )
            true
        } catch (e: Exception) {
            if (logRefusal) log.warn("Wallet change {} of player {} refused: {}", operation.cause, userId, e.message)
            false
        }

    companion object {
        /** The cause of a change whose caller names none; the currency is the entry's own field. */
        const val UNNAMED_CAUSE = "wallet.change"
    }
}
