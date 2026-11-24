package zel.protocol.generated

data class CommunityGoalVoteIn(
    val optionId: Int,
)

object CommunityGoalVoteInCodec : IncomingCodec<CommunityGoalVoteIn> {
    override val name = "CommunityGoalVoteIn"

    override fun read(r: PacketReader): CommunityGoalVoteIn = readCommunityGoalVoteIn(r)

    override fun write(w: PacketWriter, m: CommunityGoalVoteIn) = writeCommunityGoalVoteIn(w, m)

    private fun readCommunityGoalVoteIn(r: PacketReader): CommunityGoalVoteIn {
        val vOptionId = r.int()
        return CommunityGoalVoteIn(vOptionId)
    }

    private fun writeCommunityGoalVoteIn(w: PacketWriter, m: CommunityGoalVoteIn) {
        w.int(m.optionId)
    }
}
