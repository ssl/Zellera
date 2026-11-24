package zel.protocol.generated

data object GetCommunityGoalEarnedPrizesIn

object GetCommunityGoalEarnedPrizesInCodec : IncomingCodec<GetCommunityGoalEarnedPrizesIn> {
    override val name = "GetCommunityGoalEarnedPrizesIn"

    override fun read(r: PacketReader): GetCommunityGoalEarnedPrizesIn = GetCommunityGoalEarnedPrizesIn

    override fun write(w: PacketWriter, m: GetCommunityGoalEarnedPrizesIn) = writeGetCommunityGoalEarnedPrizesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCommunityGoalEarnedPrizesIn(w: PacketWriter, m: GetCommunityGoalEarnedPrizesIn) {
    }
}
