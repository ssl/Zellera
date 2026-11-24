package zel.protocol.generated

data class ClaimRewardTrackPrizeIn(
    val trackId: String,
    val prizeId: String,
)

object ClaimRewardTrackPrizeInCodec : IncomingCodec<ClaimRewardTrackPrizeIn> {
    override val name = "ClaimRewardTrackPrizeIn"

    override fun read(r: PacketReader): ClaimRewardTrackPrizeIn = readClaimRewardTrackPrizeIn(r)

    override fun write(w: PacketWriter, m: ClaimRewardTrackPrizeIn) = writeClaimRewardTrackPrizeIn(w, m)

    private fun readClaimRewardTrackPrizeIn(r: PacketReader): ClaimRewardTrackPrizeIn {
        val vTrackId = r.string()
        val vPrizeId = r.string()
        return ClaimRewardTrackPrizeIn(vTrackId, vPrizeId)
    }

    private fun writeClaimRewardTrackPrizeIn(w: PacketWriter, m: ClaimRewardTrackPrizeIn) {
        w.string(m.trackId)
        w.string(m.prizeId)
    }
}
