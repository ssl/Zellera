package zel.protocol.generated

data class RewardTracksOut(
    val boolean1: Boolean,
    val rest: ByteArray,
)

object RewardTracksOutCodec : OutgoingCodec<RewardTracksOut> {
    override val name = "RewardTracksOut"

    override fun read(r: PacketReader): RewardTracksOut = readRewardTracksOut(r)

    override fun write(w: PacketWriter, m: RewardTracksOut) = writeRewardTracksOut(w, m)

    private fun readRewardTracksOut(r: PacketReader): RewardTracksOut {
        val vBoolean1 = r.boolean()
        val vRest = r.rest()
        return RewardTracksOut(vBoolean1, vRest)
    }

    private fun writeRewardTracksOut(w: PacketWriter, m: RewardTracksOut) {
        w.boolean(m.boolean1)
        w.bytes(m.rest)
    }
}
