package zel.protocol.generated

data object TargetedOfferNotFoundOut

object TargetedOfferNotFoundOutCodec : OutgoingCodec<TargetedOfferNotFoundOut> {
    override val name = "TargetedOfferNotFoundOut"

    override fun read(r: PacketReader): TargetedOfferNotFoundOut = TargetedOfferNotFoundOut

    override fun write(w: PacketWriter, m: TargetedOfferNotFoundOut) = writeTargetedOfferNotFoundOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeTargetedOfferNotFoundOut(w: PacketWriter, m: TargetedOfferNotFoundOut) {
    }
}
