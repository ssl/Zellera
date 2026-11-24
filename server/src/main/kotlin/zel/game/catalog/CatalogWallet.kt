package zel.game.catalog

import org.slf4j.LoggerFactory
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.outgoing.users.UserCredits

object CatalogWallet {
    private val log = LoggerFactory.getLogger(CatalogWallet::class.java)

    @JvmStatic
    fun charge(
        player: Player,
        credits: Int,
        pointsType: Int,
        points: Int,
    ): Boolean {
        val charged = debit(player, credits, pointsType, points, warn = true)
        if (charged) showBalance(player, credits, pointsType, -points)
        return charged
    }

    @JvmStatic
    fun chargeQuietly(
        player: Player,
        credits: Int,
        pointsType: Int,
        points: Int,
    ): Boolean = debit(player, credits, pointsType, points, warn = false)

    @JvmStatic
    fun refund(
        player: Player,
        credits: Int,
        pointsType: Int,
        points: Int,
    ) {
        require(credits >= 0 && points >= 0) { "a catalog refund cannot be negative" }
        if (post(player, "shop.refund", credits, pointsType, points, warn = true)) {
            showBalance(player, credits, pointsType, points)
        }
    }

    /** A negative price is refused: it would pay the player. */
    private fun debit(
        player: Player,
        credits: Int,
        pointsType: Int,
        points: Int,
        warn: Boolean,
    ): Boolean =
        credits >= 0 && points >= 0 &&
            post(player, "shop.hold", -credits, pointsType, -points, warn)

    /** Tells the client the balances after a payment that was booked elsewhere. */
    fun show(
        player: Player,
        credits: Int,
        pointsType: Int,
        points: Int,
    ) = showBalance(player, credits, pointsType, points)

    private fun showBalance(
        player: Player,
        credits: Int,
        pointsType: Int,
        pointsDelta: Int,
    ) {
        val client = player.client ?: return
        if (credits != 0) client.send(UserCredits(player))
        if (pointsDelta !=
            0
        ) {
            client.send(
                ActivityPointNotificationOutCodec,
                ActivityPointNotificationOut(player.profile.balance(pointsType), pointsDelta, pointsType),
            )
        }
    }

    @Suppress("TooGenericExceptionCaught") // a refused post takes nothing
    private fun post(
        player: Player,
        cause: String,
        credits: Int,
        pointsType: Int,
        points: Int,
        warn: Boolean,
    ): Boolean {
        val playerId = player.profile.id
        val operations =
            listOfNotNull(
                credits.takeIf { it != 0 }?.let { entry(playerId, cause, EconomyLedger.CREDITS, it) },
                points.takeIf { it != 0 }?.let { entry(playerId, cause, pointsType, it) },
            )
        if (operations.isEmpty()) return true
        return try {
            WalletWrites.postAll(player, operations)
            true
        } catch (e: Exception) {
            if (warn) log.warn("{} of player {} refused: {}", cause, playerId, e.message)
            false
        }
    }

    private fun entry(
        playerId: Int,
        cause: String,
        currency: Int,
        amount: Int,
    ) = LedgerEntry(LedgerEntry.newKey("shop/$playerId"), playerId, currency, amount, cause)
}
