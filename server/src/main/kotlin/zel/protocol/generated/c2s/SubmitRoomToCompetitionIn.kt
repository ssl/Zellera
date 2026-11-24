package zel.protocol.generated

data class SubmitRoomToCompetitionIn(
    val goalCode: String,
    val level: Int,
)

object SubmitRoomToCompetitionInCodec : IncomingCodec<SubmitRoomToCompetitionIn> {
    override val name = "SubmitRoomToCompetitionIn"

    override fun read(r: PacketReader): SubmitRoomToCompetitionIn = readSubmitRoomToCompetitionIn(r)

    override fun write(w: PacketWriter, m: SubmitRoomToCompetitionIn) = writeSubmitRoomToCompetitionIn(w, m)

    private fun readSubmitRoomToCompetitionIn(r: PacketReader): SubmitRoomToCompetitionIn {
        val vGoalCode = r.string()
        val vLevel = r.int()
        return SubmitRoomToCompetitionIn(vGoalCode, vLevel)
    }

    private fun writeSubmitRoomToCompetitionIn(w: PacketWriter, m: SubmitRoomToCompetitionIn) {
        w.string(m.goalCode)
        w.int(m.level)
    }
}
