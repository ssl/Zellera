package zel.protocol.incoming.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.config.DisplayNames
import zel.db.SqlFailure
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.catalog.BalanceShortfall
import zel.game.catalog.BuildPassBuyPages
import zel.game.catalog.BuildPassMembership
import zel.game.catalog.CatalogPage
import zel.game.catalog.CatalogServices
import zel.game.catalog.CatalogWallet
import zel.game.catalog.MembershipOffer
import zel.game.catalog.Prices
import zel.game.players.Player
import zel.game.players.subscriptions.Subscription
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchaseNotAllowed
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import java.util.concurrent.ConcurrentHashMap

internal class GiftClubOffer(
    private val services: CatalogServices,
) {
    private val receiverLocks = ConcurrentHashMap<Int, Any>()

    @Suppress("ReturnCount") // one early answer per refusal
    fun gift(
        client: ClientConnection,
        sender: Player,
        page: CatalogPage,
        request: GiftRequest,
        receiverId: Int,
    ) {
        // a membership is bought for oneself in the club shop, where it also confirms and shows the new status
        if (receiverId == sender.profile.id) {
            return CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.REFUSED)
        }
        if (!page.opensFor(sender.profile.rank.id)) {
            return CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.REFUSED)
        }
        if (page.info.clubOnly && !sender.stats.club.isMember) {
            return CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.CLUB_ONLY)
        }
        val offer = services.catalog.membershipOffers.forPage(page, request.itemId)
        if (offer == null || !offer.kind.addsDays) return CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
        if (!offer.giftable) return CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
        val seconds =
            try {
                Prices.membershipSeconds(offer.price, offer.days)
            } catch (_: IllegalArgumentException) {
                return CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.REFUSED)
            }
        if (seconds <= 0) return CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)

        val price = offer.price
        val profile = sender.profile
        BalanceShortfall
            .of(price.credits, price.points, price.pointsType, profile.credits, profile.balance(price.pointsType))
            ?.let { return it.send(client) }
        if (!CatalogWallet.charge(sender, price.credits, price.pointsType, price.points)) {
            return CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.REFUSED)
        }
        if (!extendOrRefund(sender, offer, receiverId, seconds)) {
            return CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
        }
        services.players
            .find(receiverId)
            ?.client
            ?.send(notice(sender, offer))
        CatalogReplies.purchaseOk(client, purchaseOk(page, offer, request.itemId))
        services.achievements.count(sender, GameEvents.CATALOG_GIFT_SENT)
    }

    private fun extendOrRefund(
        sender: Player,
        offer: MembershipOffer,
        receiverId: Int,
        seconds: Int,
    ): Boolean {
        val price = offer.price
        val extended =
            try {
                extend(offer, receiverId, seconds)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: RuntimeException, // refunded, then rethrown
            ) {
                CatalogWallet.refund(sender, price.credits, price.pointsType, price.points)
                throw e
            }
        if (!extended) CatalogWallet.refund(sender, price.credits, price.pointsType, price.points)
        return extended
    }

    private fun extend(
        offer: MembershipOffer,
        receiverId: Int,
        seconds: Int,
    ): Boolean {
        val type = subscriptionType(offer)
        val bonus = buildPassBonus(offer)
        services.players.find(receiverId)?.let { return extendOnline(it, type, seconds, bonus) }
        val subscriptions = services.game.subscriptions ?: return false
        return synchronized(receiverLocks.computeIfAbsent(receiverId) { Any() }) {
            val running =
                subscriptions
                    .ofPlayer(receiverId)
                    .filter { it.type == type && it.isActive && it.secondsLeft > 0 }
                    .maxByOrNull { it.endsAt }
            val grown =
                running?.also { it.extend(seconds) {} }
                    ?: subscriptions.open(receiverId, type, services.now(), seconds)?.also { it.started() }
            if (grown != null && bonus > 0) grantBonusOffline(receiverId, bonus)
            grown != null
        }
    }

    private fun extendOnline(
        receiver: Player,
        type: String,
        seconds: Int,
        bonus: Int,
    ): Boolean {
        val club = receiver.stats.club
        if (bonus > 0 && !club.addBuildPassBonusFurni(bonus)) return false
        val created = club.create(type, seconds) != null
        if (created) {
            if (bonus > 0) receiver.stats.run()
        } else if (bonus > 0) {
            club.buildPassBonusFurni -= bonus
        }
        return created
    }

    private fun buildPassBonus(offer: MembershipOffer): Int =
        if (offer.kind == MembershipOffer.Kind.BUILD_PASS) BuildPassMembership.monthFurniBonus(offer.days) else 0

    private fun grantBonusOffline(
        receiverId: Int,
        bonus: Int,
    ) {
        try {
            services.database.queries.write(ADD_BONUS_FURNI_SQL, bonus, receiverId)
        } catch (e: SqlFailure) {
            log.error("Adding the gifted build pass bonus furni for player {} failed", receiverId, e)
        }
    }

    private fun subscriptionType(offer: MembershipOffer): String =
        if (offer.kind == MembershipOffer.Kind.BUILD_PASS) Subscription.BUILD_PASS else Subscription.CLUB

    private fun notice(
        sender: Player,
        offer: MembershipOffer,
    ): BubbleAlertMessage {
        val club = if (offer.kind == MembershipOffer.Kind.BUILD_PASS) DisplayNames.buildPass else DisplayNames.club
        val template =
            if (offer.days == 1) {
                services.texts.textOr(ONE_DAY_KEY, ONE_DAY_NOTICE)
            } else {
                services.texts.textOr(DAYS_KEY, DAYS_NOTICE)
            }
        val text =
            template
                .replace("%player%", sender.profile.name)
                .replace("%days%", offer.days.toString())
                .replace("%club%", club)
        return BubbleAlertMessage(BubbleAlert.GIFT_RECEIVED.key, GiftNotices.of(text))
    }

    private fun purchaseOk(
        page: CatalogPage,
        offer: MembershipOffer,
        itemId: Int,
    ) = (if (BuildPassBuyPages.isBuyPage(page)) page.offer(itemId) else null)
        ?.let { CatalogMessages.purchaseOkClubPage(it, offer) }
        ?: CatalogMessages.purchaseOkClubOffer(offer)

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GiftClubOffer::class.java)
        const val ONE_DAY_KEY = "catalog.gift.club.day"
        const val ONE_DAY_NOTICE = "%player% gave you %days% day of %club%"
        const val DAYS_KEY = "catalog.gift.club.days"
        const val DAYS_NOTICE = "%player% gave you %days% days of %club%"
        const val ADD_BONUS_FURNI_SQL =
            "UPDATE player_stats SET build_pass_bonus_furni = build_pass_bonus_furni + ? WHERE player_id = ?"
    }
}
