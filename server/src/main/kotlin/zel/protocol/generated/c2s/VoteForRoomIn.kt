package zel.protocol.generated

data class VoteForRoomIn(
    val goalCode: String,
)

object VoteForRoomInCodec : IncomingCodec<VoteForRoomIn> {
    override val name = "VoteForRoomIn"

    override fun read(r: PacketReader): VoteForRoomIn = readVoteForRoomIn(r)

    override fun write(w: PacketWriter, m: VoteForRoomIn) = writeVoteForRoomIn(w, m)

    private fun readVoteForRoomIn(r: PacketReader): VoteForRoomIn {
        val vGoalCode = r.string()
        return VoteForRoomIn(vGoalCode)
    }

    private fun writeVoteForRoomIn(w: PacketWriter, m: VoteForRoomIn) {
        w.string(m.goalCode)
    }
}
