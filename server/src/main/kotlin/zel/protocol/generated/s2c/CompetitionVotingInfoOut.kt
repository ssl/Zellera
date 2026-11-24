package zel.protocol.generated

data class CompetitionVotingInfoOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
)

object CompetitionVotingInfoOutCodec : OutgoingCodec<CompetitionVotingInfoOut> {
    override val name = "CompetitionVotingInfoOut"

    override fun read(r: PacketReader): CompetitionVotingInfoOut = readCompetitionVotingInfoOut(r)

    override fun write(w: PacketWriter, m: CompetitionVotingInfoOut) = writeCompetitionVotingInfoOut(w, m)

    private fun readCompetitionVotingInfoOut(r: PacketReader): CompetitionVotingInfoOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return CompetitionVotingInfoOut(vInt1, vString1, vInt2, vInt3)
    }

    private fun writeCompetitionVotingInfoOut(w: PacketWriter, m: CompetitionVotingInfoOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
