package zel.game.icons

import org.slf4j.LoggerFactory
import zel.content.ClientStrings
import zel.game.catalog.BalanceShortfall
import zel.game.catalog.CatalogWallet
import zel.game.players.Player
import zel.game.players.connection
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchasedOffer
import zel.protocol.outgoing.users.UserCredits
import java.sql.SQLException

object IconShop {
    private val logger = LoggerFactory.getLogger(IconShop::class.java)

    const val SET_SHORT_OF_CREDITS_TYPE = 7

    fun shortfall(
        collection: Boolean,
        credits: Int,
        points: Int,
        pointsType: Int,
        creditBalance: Int,
        pointsBalance: Int,
    ): BalanceShortfall? {
        val shortfall = BalanceShortfall.of(credits, points, pointsType, creditBalance, pointsBalance)
        return if (collection && shortfall != null && shortfall.credits) {
            BalanceShortfall(true, false, SET_SHORT_OF_CREDITS_TYPE)
        } else {
            shortfall
        }
    }

    @Suppress("ReturnCount", "LongMethod") // each early return is one refusal in the purchase
    fun buy(
        player: Player,
        collection: Boolean,
        id: Int,
    ) {
        val service = player.iconService
        val userId = player.profile.id
        val allUnlocked = IconService.allUnlocked(player)
        val purchase =
            try {
                val snapshot = service.rosterOf(userId, allUnlocked)
                if (collection) snapshot.offerForSet(id) else snapshot.offerFor(id)
            } catch (_: IconRejected) {
                return
            } catch (exception: SQLException) {
                logger.error("Unable to load icons of user {}", userId, exception)
                return
            }

        val credits = purchase.price.credits
        val points = purchase.price.points
        val pointsType = purchase.price.pointsType
        val shortfall =
            shortfall(
                collection,
                credits,
                points,
                pointsType,
                player.profile.credits,
                player.profile.balance(pointsType),
            )
        if (shortfall != null) {
            shortfall.send(checkNotNull(player.client) { "a buyer has a client" })
            return
        }
        if (!CatalogWallet.chargeQuietly(player, credits, pointsType, points)) {
            CatalogReplies.purchaseFailed(player.connection, PurchaseError.GENERIC)
            return
        }

        val outcome =
            try {
                service.grantPurchase(userId, allUnlocked, purchase)
            } catch (_: IconRejected) {
                CatalogWallet.refund(player, credits, pointsType, points)
                return
            } catch (exception: SQLException) {
                logger.error("Unable to grant bought icons {} to user {}", purchase.ids, userId, exception)
                CatalogWallet.refund(player, credits, pointsType, points)
                CatalogReplies.purchaseFailed(player.connection, PurchaseError.GENERIC)
                return
            }

        if (points > 0) {
            player.client?.send(
                ActivityPointNotificationOutCodec,
                ActivityPointNotificationOut(player.profile.balance(pointsType), -points, pointsType),
            )
        }
        service.countCollected(player, outcome)
        if (credits > 0) player.client?.send(UserCredits(player))
        IconService.announceStates(player, outcome)
        CatalogReplies.purchaseOk(player.connection, CatalogMessages.purchaseOkOffer(offer(purchase)))
    }

    fun offer(purchase: IconPurchase): PurchasedOffer {
        val price = purchase.price
        return PurchasedOffer(
            purchase.offerId,
            purchase.localizationId,
            price.credits,
            price.points,
            price.pointsType,
            ClientStrings[ClientStrings.ICON_PRODUCT_TYPE].orEmpty(),
            purchase.ids.map(Int::toString),
        )
    }
}
