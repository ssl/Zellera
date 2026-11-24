package zel.protocol.generated

data object GetCommunityGoalProgressIn

object GetCommunityGoalProgressInCodec : IncomingCodec<GetCommunityGoalProgressIn> {
    override val name = "GetCommunityGoalProgressIn"

    override fun read(r: PacketReader): GetCommunityGoalProgressIn = GetCommunityGoalProgressIn

    override fun write(w: PacketWriter, m: GetCommunityGoalProgressIn) = writeGetCommunityGoalProgressIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCommunityGoalProgressIn(w: PacketWriter, m: GetCommunityGoalProgressIn) {
    }
}
