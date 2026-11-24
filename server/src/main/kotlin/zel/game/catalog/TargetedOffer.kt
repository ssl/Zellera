package zel.game.catalog

import zel.game.players.PlayerOfferPurchase
import zel.protocol.generated.TargetedOfferOut
import java.sql.ResultSet

class TargetedOffer private constructor(
    val id: Int,
    val code: String,
    val title: String,
    val description: String,
    val image: String,
    val icon: String,
    val endsAt: Int,
    val price: OfferPrice,
    val purchaseLimit: Int,
    val catalogOfferId: Int,
    val productCodes: List<String>,
    private val services: CatalogServices,
) {
    val isRunning: Boolean get() = endsAt > services.now()

    fun message(purchase: PlayerOfferPurchase): TargetedOfferOut =
        TargetedOfferOut(
            int1 = purchase.state,
            int2 = id,
            string1 = code,
            string2 = productCodes.firstOrNull().orEmpty(),
            int3 = price.credits,
            int4 = price.points,
            int5 = price.pointsType,
            int6 = maxOf(purchaseLimit - purchase.bought, 0),
            int7 = maxOf(0, endsAt - services.now()),
            string3 = title,
            string4 = description,
            string5 = image,
            string6 = icon,
            int8 = 0,
            list1 = productCodes,
        )

    companion object {
        private fun storedPrice(row: ResultSet): OfferPrice =
            OfferPrice.stored(row.getInt("price_credits"), row.getInt("price_currency"), row.getInt("currency"))

        fun purchasableAmount(
            requested: Int,
            limit: Int,
            alreadyBought: Int,
        ): Int {
            if (requested <= 0 || limit <= 0) return 0
            val left = limit - maxOf(0, alreadyBought)
            return requested.coerceIn(0, maxOf(0, left))
        }

        fun fromRow(
            row: ResultSet,
            services: CatalogServices,
        ): TargetedOffer =
            TargetedOffer(
                id = row.getInt("id"),
                code = row.getString("code").orEmpty(),
                title = row.getString("title").orEmpty(),
                description = row.getString("description").orEmpty(),
                image = row.getString("image").orEmpty(),
                icon = row.getString("icon").orEmpty(),
                endsAt = row.getInt("ends_at"),
                price = storedPrice(row),
                purchaseLimit = row.getInt("purchase_limit"),
                catalogOfferId = row.getInt("offer_id"),
                productCodes =
                    row
                        .getString("vars")
                        .orEmpty()
                        .split(";")
                        .filter { it.isNotEmpty() },
                services = services,
            )
    }
}
