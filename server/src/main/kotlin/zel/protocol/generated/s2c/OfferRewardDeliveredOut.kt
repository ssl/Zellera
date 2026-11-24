package zel.protocol.generated

data class OfferRewardDeliveredOut(
    val string1: String,
    val int1: Int,
    val string2: String,
    val string3: String,
)

object OfferRewardDeliveredOutCodec : OutgoingCodec<OfferRewardDeliveredOut> {
    override val name = "OfferRewardDeliveredOut"

    override fun read(r: PacketReader): OfferRewardDeliveredOut = readOfferRewardDeliveredOut(r)

    override fun write(w: PacketWriter, m: OfferRewardDeliveredOut) = writeOfferRewardDeliveredOut(w, m)

    private fun readOfferRewardDeliveredOut(r: PacketReader): OfferRewardDeliveredOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        return OfferRewardDeliveredOut(vString1, vInt1, vString2, vString3)
    }

    private fun writeOfferRewardDeliveredOut(w: PacketWriter, m: OfferRewardDeliveredOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.string(m.string3)
    }
}
