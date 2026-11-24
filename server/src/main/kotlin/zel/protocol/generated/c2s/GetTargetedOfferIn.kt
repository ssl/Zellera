package zel.protocol.generated

data class GetTargetedOfferIn(
    val int1: Int,
)

object GetTargetedOfferInCodec : IncomingCodec<GetTargetedOfferIn> {
    override val name = "GetTargetedOfferIn"

    override fun read(r: PacketReader): GetTargetedOfferIn = readGetTargetedOfferIn(r)

    override fun write(w: PacketWriter, m: GetTargetedOfferIn) = writeGetTargetedOfferIn(w, m)

    private fun readGetTargetedOfferIn(r: PacketReader): GetTargetedOfferIn {
        val vInt1 = r.int()
        return GetTargetedOfferIn(vInt1)
    }

    private fun writeGetTargetedOfferIn(w: PacketWriter, m: GetTargetedOfferIn) {
        w.int(m.int1)
    }
}
