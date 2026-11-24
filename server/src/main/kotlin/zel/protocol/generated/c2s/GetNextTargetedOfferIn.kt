package zel.protocol.generated

data object GetNextTargetedOfferIn

object GetNextTargetedOfferInCodec : IncomingCodec<GetNextTargetedOfferIn> {
    override val name = "GetNextTargetedOfferIn"

    override fun read(r: PacketReader): GetNextTargetedOfferIn = GetNextTargetedOfferIn

    override fun write(w: PacketWriter, m: GetNextTargetedOfferIn) = writeGetNextTargetedOfferIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetNextTargetedOfferIn(w: PacketWriter, m: GetNextTargetedOfferIn) {
    }
}
