package zel.protocol.generated

data class IsPlayerPartOfCompetitionOut(
    val boolean1: Boolean,
    val int1: Int,
)

object IsPlayerPartOfCompetitionOutCodec : OutgoingCodec<IsPlayerPartOfCompetitionOut> {
    override val name = "IsPlayerPartOfCompetitionOut"

    override fun read(r: PacketReader): IsPlayerPartOfCompetitionOut = readIsPlayerPartOfCompetitionOut(r)

    override fun write(w: PacketWriter, m: IsPlayerPartOfCompetitionOut) = writeIsPlayerPartOfCompetitionOut(w, m)

    private fun readIsPlayerPartOfCompetitionOut(r: PacketReader): IsPlayerPartOfCompetitionOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        return IsPlayerPartOfCompetitionOut(vBoolean1, vInt1)
    }

    private fun writeIsPlayerPartOfCompetitionOut(w: PacketWriter, m: IsPlayerPartOfCompetitionOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
    }
}
