package zel.protocol.generated

data class LimitedOfferAppearingNextOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val string1: String,
)

object LimitedOfferAppearingNextOutCodec : OutgoingCodec<LimitedOfferAppearingNextOut> {
    override val name = "LimitedOfferAppearingNextOut"

    override fun read(r: PacketReader): LimitedOfferAppearingNextOut = readLimitedOfferAppearingNextOut(r)

    override fun write(w: PacketWriter, m: LimitedOfferAppearingNextOut) = writeLimitedOfferAppearingNextOut(w, m)

    private fun readLimitedOfferAppearingNextOut(r: PacketReader): LimitedOfferAppearingNextOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        return LimitedOfferAppearingNextOut(vInt1, vInt2, vInt3, vString1)
    }

    private fun writeLimitedOfferAppearingNextOut(w: PacketWriter, m: LimitedOfferAppearingNextOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
    }
}
