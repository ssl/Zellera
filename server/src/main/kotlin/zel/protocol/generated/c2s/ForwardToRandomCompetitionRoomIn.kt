package zel.protocol.generated

data class ForwardToRandomCompetitionRoomIn(
    val goalCode: String,
)

object ForwardToRandomCompetitionRoomInCodec : IncomingCodec<ForwardToRandomCompetitionRoomIn> {
    override val name = "ForwardToRandomCompetitionRoomIn"

    override fun read(r: PacketReader): ForwardToRandomCompetitionRoomIn = readForwardToRandomCompetitionRoomIn(r)

    override fun write(w: PacketWriter, m: ForwardToRandomCompetitionRoomIn) = writeForwardToRandomCompetitionRoomIn(w, m)

    private fun readForwardToRandomCompetitionRoomIn(r: PacketReader): ForwardToRandomCompetitionRoomIn {
        val vGoalCode = r.string()
        return ForwardToRandomCompetitionRoomIn(vGoalCode)
    }

    private fun writeForwardToRandomCompetitionRoomIn(w: PacketWriter, m: ForwardToRandomCompetitionRoomIn) {
        w.string(m.goalCode)
    }
}
