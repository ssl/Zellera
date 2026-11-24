package zel.protocol.outgoing.catalog

data class PurchasedOffer
    constructor(
        val offerId: Int,
        val localizationId: String,
        val credits: Int,
        val points: Int,
        val pointsType: Int,
        val productType: String,
        val productParams: List<String>,
        val clubLevel: Int = 0,
    ) {
        companion object {
            const val CREATE_GUILD_OFFER_ID = 6247

            fun guild(credits: Int) =
                PurchasedOffer(CREATE_GUILD_OFFER_ID, "CREATE_GUILD", credits, 0, 0, "", emptyList(), 2)
        }
    }
