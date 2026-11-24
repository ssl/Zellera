package zel.protocol.generated

data class ForwardToACompetitionRoomIn(
    val goalCode: String,
    val roomId: Int,
)

object ForwardToACompetitionRoomInCodec : IncomingCodec<ForwardToACompetitionRoomIn> {
    override val name = "ForwardToACompetitionRoomIn"

    override fun read(r: PacketReader): ForwardToACompetitionRoomIn = readForwardToACompetitionRoomIn(r)

    override fun write(w: PacketWriter, m: ForwardToACompetitionRoomIn) = writeForwardToACompetitionRoomIn(w, m)

    private fun readForwardToACompetitionRoomIn(r: PacketReader): ForwardToACompetitionRoomIn {
        val vGoalCode = r.string()
        val vRoomId = r.int()
        return ForwardToACompetitionRoomIn(vGoalCode, vRoomId)
    }

    private fun writeForwardToACompetitionRoomIn(w: PacketWriter, m: ForwardToACompetitionRoomIn) {
        w.string(m.goalCode)
        w.int(m.roomId)
    }
}
