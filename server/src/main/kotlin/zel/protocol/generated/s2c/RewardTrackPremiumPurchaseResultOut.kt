package zel.protocol.generated

data class RewardTrackPremiumPurchaseResultOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
)

object RewardTrackPremiumPurchaseResultOutCodec : OutgoingCodec<RewardTrackPremiumPurchaseResultOut> {
    override val name = "RewardTrackPremiumPurchaseResultOut"

    override fun read(r: PacketReader): RewardTrackPremiumPurchaseResultOut = readRewardTrackPremiumPurchaseResultOut(r)

    override fun write(w: PacketWriter, m: RewardTrackPremiumPurchaseResultOut) = writeRewardTrackPremiumPurchaseResultOut(w, m)

    private fun readRewardTrackPremiumPurchaseResultOut(r: PacketReader): RewardTrackPremiumPurchaseResultOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RewardTrackPremiumPurchaseResultOut(vString1, vInt1, vInt2)
    }

    private fun writeRewardTrackPremiumPurchaseResultOut(w: PacketWriter, m: RewardTrackPremiumPurchaseResultOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
    }
}
