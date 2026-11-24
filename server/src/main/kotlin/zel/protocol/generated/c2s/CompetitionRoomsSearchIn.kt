package zel.protocol.generated

data class CompetitionRoomsSearchIn(
    val goalId: Int,
    val pageIndex: Int,
)

object CompetitionRoomsSearchInCodec : IncomingCodec<CompetitionRoomsSearchIn> {
    override val name = "CompetitionRoomsSearchIn"

    override fun read(r: PacketReader): CompetitionRoomsSearchIn = readCompetitionRoomsSearchIn(r)

    override fun write(w: PacketWriter, m: CompetitionRoomsSearchIn) = writeCompetitionRoomsSearchIn(w, m)

    private fun readCompetitionRoomsSearchIn(r: PacketReader): CompetitionRoomsSearchIn {
        val vGoalId = r.int()
        val vPageIndex = r.int()
        return CompetitionRoomsSearchIn(vGoalId, vPageIndex)
    }

    private fun writeCompetitionRoomsSearchIn(w: PacketWriter, m: CompetitionRoomsSearchIn) {
        w.int(m.goalId)
        w.int(m.pageIndex)
    }
}
