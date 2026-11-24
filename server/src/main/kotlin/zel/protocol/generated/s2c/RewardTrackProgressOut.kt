package zel.protocol.generated

data class RewardTrackProgressOut(
    val string1: String,
    val string2: String,
    val int1: Int,
    val int2: Int,
)

object RewardTrackProgressOutCodec : OutgoingCodec<RewardTrackProgressOut> {
    override val name = "RewardTrackProgressOut"

    override fun read(r: PacketReader): RewardTrackProgressOut = readRewardTrackProgressOut(r)

    override fun write(w: PacketWriter, m: RewardTrackProgressOut) = writeRewardTrackProgressOut(w, m)

    private fun readRewardTrackProgressOut(r: PacketReader): RewardTrackProgressOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RewardTrackProgressOut(vString1, vString2, vInt1, vInt2)
    }

    private fun writeRewardTrackProgressOut(w: PacketWriter, m: RewardTrackProgressOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
        w.int(m.int2)
    }
}
