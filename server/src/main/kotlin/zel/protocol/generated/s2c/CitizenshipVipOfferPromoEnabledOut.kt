package zel.protocol.generated

data object CitizenshipVipOfferPromoEnabledOut

object CitizenshipVipOfferPromoEnabledOutCodec : OutgoingCodec<CitizenshipVipOfferPromoEnabledOut> {
    override val name = "CitizenshipVipOfferPromoEnabledOut"

    override fun read(r: PacketReader): CitizenshipVipOfferPromoEnabledOut = CitizenshipVipOfferPromoEnabledOut

    override fun write(w: PacketWriter, m: CitizenshipVipOfferPromoEnabledOut) = writeCitizenshipVipOfferPromoEnabledOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCitizenshipVipOfferPromoEnabledOut(w: PacketWriter, m: CitizenshipVipOfferPromoEnabledOut) {
    }
}
