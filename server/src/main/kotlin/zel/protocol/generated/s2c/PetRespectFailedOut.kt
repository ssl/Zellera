package zel.protocol.generated

data class PetRespectFailedOut(
    val int1: Int,
    val int2: Int,
)

object PetRespectFailedOutCodec : OutgoingCodec<PetRespectFailedOut> {
    override val name = "PetRespectFailedOut"

    override fun read(r: PacketReader): PetRespectFailedOut = readPetRespectFailedOut(r)

    override fun write(w: PacketWriter, m: PetRespectFailedOut) = writePetRespectFailedOut(w, m)

    private fun readPetRespectFailedOut(r: PacketReader): PetRespectFailedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PetRespectFailedOut(vInt1, vInt2)
    }

    private fun writePetRespectFailedOut(w: PacketWriter, m: PetRespectFailedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
