package zel.protocol.generated

data class CompetitionStatusOut(
    val boolean1: Boolean,
    val string1: String,
)

object CompetitionStatusOutCodec : OutgoingCodec<CompetitionStatusOut> {
    override val name = "CompetitionStatusOut"

    override fun read(r: PacketReader): CompetitionStatusOut = readCompetitionStatusOut(r)

    override fun write(w: PacketWriter, m: CompetitionStatusOut) = writeCompetitionStatusOut(w, m)

    private fun readCompetitionStatusOut(r: PacketReader): CompetitionStatusOut {
        val vBoolean1 = r.boolean()
        val vString1 = r.string()
        return CompetitionStatusOut(vBoolean1, vString1)
    }

    private fun writeCompetitionStatusOut(w: PacketWriter, m: CompetitionStatusOut) {
        w.boolean(m.boolean1)
        w.string(m.string1)
    }
}
