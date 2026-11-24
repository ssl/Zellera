package zel.game.catalog

import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.VoucherError
import zel.util.SECONDS_PER_MINUTE
import org.slf4j.LoggerFactory

class VoucherService(
    private val store: VoucherStore,
    private val catalog: Catalog,
    private val lockout: VoucherLockout,
    private val lockedText: () -> String,
) {
    fun redeem(
        client: ClientConnection,
        typed: String,
    ) {
        val player = client.player ?: return
        val playerId = player.profile.id
        val wait = lockout.secondsLeft(playerId)
        if (wait > 0) {
            val minutes = (wait + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
            return player.alert(lockedText().replace("%minutes%", minutes.toString()))
        }
        val code = typed.trim()
        if (code.isEmpty() || code.length > MAX_CODE || code.any(Char::isWhitespace)) {
            // the client's text for this answer asks for a code without spaces
            return CatalogReplies.voucherError(client, VoucherError.TECHNICAL)
        }
        when (val claim = claim(player, code)) {
            is VoucherStore.Claim.Taken -> {
                lockout.redeemed(playerId)
                handOut(client, player, claim.voucher)
            }

            VoucherStore.Claim.NoSuchCode -> {
                lockout.wrongCode(playerId)
                CatalogReplies.voucherError(client, VoucherError.UNKNOWN_CODE)
            }

            VoucherStore.Claim.Spent -> {
                CatalogReplies.voucherError(client, VoucherError.UNKNOWN_CODE)
            }

            null -> {
                CatalogReplies.voucherError(client, VoucherError.TECHNICAL)
            }
        }
    }

    /** Null when the claim broke off; nothing is used or paid then. */
    @Suppress("TooGenericExceptionCaught") // a failed statement and a balance that does not fit both pay nothing
    private fun claim(
        player: Player,
        code: String,
    ): VoucherStore.Claim? =
        try {
            WalletWrites.locked(player) {
                store.claim(player.profile.id, code).also { claim ->
                    if (claim is VoucherStore.Claim.Taken) {
                        claim.balances.forEach { (currency, balance) ->
                            WalletWrites.publish(player, currency, balance)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Voucher claim of player {} broke off", player.profile.id, e)
            null
        }

    private fun handOut(
        client: ClientConnection,
        player: Player,
        voucher: VoucherCode,
    ) {
        CatalogWallet.show(player, voucher.credits, voucher.pointsType, voucher.points)
        val offer = voucher.offerId.takeIf { it > 0 }?.let(catalog::offer)
        if (offer == null) return CatalogReplies.voucherRedeemed(client)
        if (catalog.purchase(offer, player, 1, "", true)) {
            val product = offer.products.firstOrNull()
            return CatalogReplies.voucherRedeemed(client, product?.displayName.orEmpty())
        }
        if (voucher.credits > 0 || voucher.points > 0) {
            log.warn("Voucher {} paid player {} but not its offer {}", voucher.id, player.profile.id, offer.id)
            return CatalogReplies.voucherRedeemed(client)
        }
        giveBack(voucher, player)
        CatalogReplies.voucherError(client, VoucherError.TECHNICAL)
    }

    @Suppress("TooGenericExceptionCaught") // the use stays counted; the log says for whom
    private fun giveBack(
        voucher: VoucherCode,
        player: Player,
    ) {
        try {
            store.giveBack(voucher.id, player.profile.id)
        } catch (e: Exception) {
            log.error("Use of voucher {} by player {} not given back", voucher.id, player.profile.id, e)
        }
    }

    companion object {
        const val LOCKED_TEXT = "catalog.voucher.locked"
        private const val MAX_CODE = 32
        private val log = LoggerFactory.getLogger(VoucherService::class.java)
    }
}
