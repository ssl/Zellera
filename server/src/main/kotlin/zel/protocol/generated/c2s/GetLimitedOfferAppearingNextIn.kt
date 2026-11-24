package zel.protocol.generated

data object GetLimitedOfferAppearingNextIn

object GetLimitedOfferAppearingNextInCodec : IncomingCodec<GetLimitedOfferAppearingNextIn> {
    override val name = "GetLimitedOfferAppearingNextIn"

    override fun read(r: PacketReader): GetLimitedOfferAppearingNextIn = GetLimitedOfferAppearingNextIn

    override fun write(w: PacketWriter, m: GetLimitedOfferAppearingNextIn) = writeGetLimitedOfferAppearingNextIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetLimitedOfferAppearingNextIn(w: PacketWriter, m: GetLimitedOfferAppearingNextIn) {
    }
}
