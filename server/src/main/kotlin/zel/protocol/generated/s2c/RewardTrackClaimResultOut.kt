package zel.protocol.generated

data class RewardTrackClaimResultOut(
    val string1: String,
    val string2: String,
    val int1: Int,
)

object RewardTrackClaimResultOutCodec : OutgoingCodec<RewardTrackClaimResultOut> {
    override val name = "RewardTrackClaimResultOut"

    override fun read(r: PacketReader): RewardTrackClaimResultOut = readRewardTrackClaimResultOut(r)

    override fun write(w: PacketWriter, m: RewardTrackClaimResultOut) = writeRewardTrackClaimResultOut(w, m)

    private fun readRewardTrackClaimResultOut(r: PacketReader): RewardTrackClaimResultOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        return RewardTrackClaimResultOut(vString1, vString2, vInt1)
    }

    private fun writeRewardTrackClaimResultOut(w: PacketWriter, m: RewardTrackClaimResultOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
    }
}
