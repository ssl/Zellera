package zel.protocol.generated

data class CompetitionRoomsDataOut(
    val rest: ByteArray,
)

object CompetitionRoomsDataOutCodec : OutgoingCodec<CompetitionRoomsDataOut> {
    override val name = "CompetitionRoomsDataOut"

    override fun read(r: PacketReader): CompetitionRoomsDataOut = readCompetitionRoomsDataOut(r)

    override fun write(w: PacketWriter, m: CompetitionRoomsDataOut) = writeCompetitionRoomsDataOut(w, m)

    private fun readCompetitionRoomsDataOut(r: PacketReader): CompetitionRoomsDataOut {
        val vRest = r.rest()
        return CompetitionRoomsDataOut(vRest)
    }

    private fun writeCompetitionRoomsDataOut(w: PacketWriter, m: CompetitionRoomsDataOut) {
        w.bytes(m.rest)
    }
}
