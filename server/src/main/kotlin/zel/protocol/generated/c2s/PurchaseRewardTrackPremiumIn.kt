package zel.protocol.generated

data class PurchaseRewardTrackPremiumIn(
    val string1: String,
)

object PurchaseRewardTrackPremiumInCodec : IncomingCodec<PurchaseRewardTrackPremiumIn> {
    override val name = "PurchaseRewardTrackPremiumIn"

    override fun read(r: PacketReader): PurchaseRewardTrackPremiumIn = readPurchaseRewardTrackPremiumIn(r)

    override fun write(w: PacketWriter, m: PurchaseRewardTrackPremiumIn) = writePurchaseRewardTrackPremiumIn(w, m)

    private fun readPurchaseRewardTrackPremiumIn(r: PacketReader): PurchaseRewardTrackPremiumIn {
        val vString1 = r.string()
        return PurchaseRewardTrackPremiumIn(vString1)
    }

    private fun writePurchaseRewardTrackPremiumIn(w: PacketWriter, m: PurchaseRewardTrackPremiumIn) {
        w.string(m.string1)
    }
}
