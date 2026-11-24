package zel.protocol.outgoing.catalog

import zel.game.catalog.BuildPassMembership
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.generated.ApproveNameOut
import zel.protocol.generated.ApproveNameOutCodec
import zel.protocol.generated.BuildPassFurniCountOutCodec
import zel.protocol.generated.BuildPassSubscriptionStatusOutCodec
import zel.protocol.generated.CatalogPublishedOut
import zel.protocol.generated.CatalogPublishedOutCodec
import zel.protocol.generated.IsOfferGiftableOut
import zel.protocol.generated.IsOfferGiftableOutCodec
import zel.protocol.generated.LimitedEditionSoldOutOut
import zel.protocol.generated.LimitedEditionSoldOutOutCodec
import zel.protocol.generated.LtdRaffleEnteredOut
import zel.protocol.generated.LtdRaffleEnteredOutCodec
import zel.protocol.generated.LtdRaffleResultOut
import zel.protocol.generated.LtdRaffleResultOutCodec
import zel.protocol.generated.MarketplaceBuyOfferResultOut
import zel.protocol.generated.MarketplaceBuyOfferResultOutCodec
import zel.protocol.generated.MarketplaceCanMakeOfferResultOut
import zel.protocol.generated.MarketplaceCanMakeOfferResultOutCodec
import zel.protocol.generated.MarketplaceCancelAllOffersResultOut
import zel.protocol.generated.MarketplaceCancelAllOffersResultOutCodec
import zel.protocol.generated.MarketplaceCancelOfferResultOut
import zel.protocol.generated.MarketplaceCancelOfferResultOutCodec
import zel.protocol.generated.MarketplaceClearOwnHistoryResultOut
import zel.protocol.generated.MarketplaceClearOwnHistoryResultOutCodec
import zel.protocol.generated.MarketplaceMakeOfferResultOut
import zel.protocol.generated.MarketplaceMakeOfferResultOutCodec
import zel.protocol.generated.NotEnoughBalanceOut
import zel.protocol.generated.NotEnoughBalanceOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PurchaseErrorOut
import zel.protocol.generated.PurchaseErrorOutCodec
import zel.protocol.generated.PurchaseNotAllowedOut
import zel.protocol.generated.PurchaseNotAllowedOutCodec
import zel.protocol.generated.PurchaseOKOut
import zel.protocol.generated.PurchaseOKOutCodec
import zel.protocol.generated.RecyclerFinishedOut
import zel.protocol.generated.RecyclerFinishedOutCodec
import zel.protocol.generated.RecyclerStatusOut
import zel.protocol.generated.RecyclerStatusOutCodec
import zel.protocol.generated.VoucherRedeemErrorOut
import zel.protocol.generated.VoucherRedeemErrorOutCodec
import zel.protocol.generated.VoucherRedeemOkOut
import zel.protocol.generated.VoucherRedeemOkOutCodec

@Suppress("TooManyFunctions") // one sender per message
object CatalogReplies {
    fun <T> send(
        client: ClientConnection,
        codec: OutgoingCodec<T>,
        message: T,
    ) = client.send(codec, message)

    fun purchaseFailed(
        client: ClientConnection,
        code: PurchaseError,
    ) = send(client, PurchaseErrorOutCodec, PurchaseErrorOut(code.code))

    fun purchaseNotAllowed(
        client: ClientConnection,
        code: PurchaseNotAllowed,
    ) = send(client, PurchaseNotAllowedOutCodec, PurchaseNotAllowedOut(code.code))

    fun limitedSoldOut(client: ClientConnection) = send(client, LimitedEditionSoldOutOutCodec, LimitedEditionSoldOutOut)

    fun notEnoughBalance(
        client: ClientConnection,
        credits: Boolean,
        pixels: Boolean,
        pointsType: Int,
    ) = send(client, NotEnoughBalanceOutCodec, NotEnoughBalanceOut(credits, pixels, pointsType))

    fun catalogUpdated(client: ClientConnection) =
        send(client, CatalogPublishedOutCodec, CatalogPublishedOut(false, null))

    fun isOfferGiftable(
        client: ClientConnection,
        offerId: Int,
        giftable: Boolean,
    ) = send(client, IsOfferGiftableOutCodec, IsOfferGiftableOut(offerId, giftable))

    fun ltdRaffleEntered(
        client: ClientConnection,
        className: String?,
    ) = send(client, LtdRaffleEnteredOutCodec, LtdRaffleEnteredOut(className.orEmpty()))

    fun ltdRaffleResult(
        client: ClientConnection,
        className: String?,
        result: LtdRaffleResult,
    ) = send(client, LtdRaffleResultOutCodec, LtdRaffleResultOut(className.orEmpty(), result.code.toByte()))

    fun petNameStatus(
        client: ClientConnection,
        status: PetNameStatus,
        value: String,
    ) = send(client, ApproveNameOutCodec, ApproveNameOut(status.code, value))

    fun voucherError(
        client: ClientConnection,
        code: VoucherError,
    ) = send(client, VoucherRedeemErrorOutCodec, VoucherRedeemErrorOut(code.code.toString()))

    /** A [productName] makes the client name the furni the code gave. */
    fun voucherRedeemed(
        client: ClientConnection,
        productName: String = "",
    ) = send(client, VoucherRedeemOkOutCodec, VoucherRedeemOkOut("", productName))

    fun recyclerFinished(
        client: ClientConnection,
        code: RecyclerResult,
        prizeId: Int = 0,
    ) = send(client, RecyclerFinishedOutCodec, RecyclerFinishedOut(code.code, prizeId))

    fun recyclerStatus(
        client: ClientConnection,
        status: Int,
        secondsLeft: Int,
    ) = send(client, RecyclerStatusOutCodec, RecyclerStatusOut(status, secondsLeft))

    fun marketplaceBuyResult(
        client: ClientConnection,
        code: MarketplaceBuyResult,
        newOfferId: Int,
        newPrice: Int,
        requestedOfferId: Int,
    ) = send(
        client,
        MarketplaceBuyOfferResultOutCodec,
        MarketplaceBuyOfferResultOut(code.code, newOfferId, newPrice, requestedOfferId),
    )

    fun marketplaceCancelAllResult(
        client: ClientConnection,
        offerIds: List<Int>,
        success: Boolean,
    ) = send(
        client,
        MarketplaceCancelAllOffersResultOutCodec,
        MarketplaceCancelAllOffersResultOut(offerIds, success),
    )

    fun marketplaceCancelResult(
        client: ClientConnection,
        offerId: Int,
        success: Boolean,
    ) = send(client, MarketplaceCancelOfferResultOutCodec, MarketplaceCancelOfferResultOut(offerId, success))

    fun marketplaceClearHistoryResult(
        client: ClientConnection,
        success: Boolean,
    ) = send(client, MarketplaceClearOwnHistoryResultOutCodec, MarketplaceClearOwnHistoryResultOut(success))

    fun marketplacePostResult(
        client: ClientConnection,
        code: MarketplacePostResult,
    ) = send(client, MarketplaceMakeOfferResultOutCodec, MarketplaceMakeOfferResultOut(code.code))

    fun marketplaceSellStatus(
        client: ClientConnection,
        code: MarketplaceSellStatus,
        tokenCount: Int,
    ) = send(client, MarketplaceCanMakeOfferResultOutCodec, MarketplaceCanMakeOfferResultOut(code.code, tokenCount))

    fun purchaseOk(
        client: ClientConnection,
        message: PurchaseOKOut,
    ) = send(client, PurchaseOKOutCodec, message)

    fun buildPassFurniCount(
        client: ClientConnection,
        count: Int,
    ) = send(client, BuildPassFurniCountOutCodec, CatalogMessages.buildPassFurniCount(count))

    fun buildPassStatus(
        client: ClientConnection,
        secondsLeft: Int,
        furniLimit: Int,
        maxFurniLimit: Int,
        secondsLeftWithGrace: Int,
    ) = send(
        client,
        BuildPassSubscriptionStatusOutCodec,
        CatalogMessages.buildPassStatus(secondsLeft, furniLimit, maxFurniLimit, secondsLeftWithGrace),
    )

    fun buildPassStatus(
        client: ClientConnection,
        player: Player,
    ) {
        val profile = player.profile
        val secondsLeft = BuildPassMembership.secondsLeft(profile)
        buildPassStatus(
            client,
            secondsLeft,
            BuildPassMembership.furniLimit(profile),
            BuildPassMembership.maxFurniLimit(profile),
            secondsLeft,
        )
    }
}
