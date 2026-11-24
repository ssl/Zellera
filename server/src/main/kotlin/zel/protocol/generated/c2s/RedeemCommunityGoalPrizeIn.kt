package zel.protocol.generated

data class RedeemCommunityGoalPrizeIn(
    val int1: Int,
)

object RedeemCommunityGoalPrizeInCodec : IncomingCodec<RedeemCommunityGoalPrizeIn> {
    override val name = "RedeemCommunityGoalPrizeIn"

    override fun read(r: PacketReader): RedeemCommunityGoalPrizeIn = readRedeemCommunityGoalPrizeIn(r)

    override fun write(w: PacketWriter, m: RedeemCommunityGoalPrizeIn) = writeRedeemCommunityGoalPrizeIn(w, m)

    private fun readRedeemCommunityGoalPrizeIn(r: PacketReader): RedeemCommunityGoalPrizeIn {
        val vInt1 = r.int()
        return RedeemCommunityGoalPrizeIn(vInt1)
    }

    private fun writeRedeemCommunityGoalPrizeIn(w: PacketWriter, m: RedeemCommunityGoalPrizeIn) {
        w.int(m.int1)
    }
}
