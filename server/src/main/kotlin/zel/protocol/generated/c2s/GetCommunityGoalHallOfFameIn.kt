package zel.protocol.generated

data class GetCommunityGoalHallOfFameIn(
    val code: String,
)

object GetCommunityGoalHallOfFameInCodec : IncomingCodec<GetCommunityGoalHallOfFameIn> {
    override val name = "GetCommunityGoalHallOfFameIn"

    override fun read(r: PacketReader): GetCommunityGoalHallOfFameIn = readGetCommunityGoalHallOfFameIn(r)

    override fun write(w: PacketWriter, m: GetCommunityGoalHallOfFameIn) = writeGetCommunityGoalHallOfFameIn(w, m)

    private fun readGetCommunityGoalHallOfFameIn(r: PacketReader): GetCommunityGoalHallOfFameIn {
        val vCode = r.string()
        return GetCommunityGoalHallOfFameIn(vCode)
    }

    private fun writeGetCommunityGoalHallOfFameIn(w: PacketWriter, m: GetCommunityGoalHallOfFameIn) {
        w.string(m.code)
    }
}
