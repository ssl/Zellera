package zel.protocol.generated

data class GetIsPlayerPartOfCompetitionIn(
    val goalCode: String,
)

object GetIsPlayerPartOfCompetitionInCodec : IncomingCodec<GetIsPlayerPartOfCompetitionIn> {
    override val name = "GetIsPlayerPartOfCompetitionIn"

    override fun read(r: PacketReader): GetIsPlayerPartOfCompetitionIn = readGetIsPlayerPartOfCompetitionIn(r)

    override fun write(w: PacketWriter, m: GetIsPlayerPartOfCompetitionIn) = writeGetIsPlayerPartOfCompetitionIn(w, m)

    private fun readGetIsPlayerPartOfCompetitionIn(r: PacketReader): GetIsPlayerPartOfCompetitionIn {
        val vGoalCode = r.string()
        return GetIsPlayerPartOfCompetitionIn(vGoalCode)
    }

    private fun writeGetIsPlayerPartOfCompetitionIn(w: PacketWriter, m: GetIsPlayerPartOfCompetitionIn) {
        w.string(m.goalCode)
    }
}
