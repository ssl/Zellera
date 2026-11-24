package zel.protocol.generated

data class PetPlacingErrorOut(
    val int1: Int,
)

object PetPlacingErrorOutCodec : OutgoingCodec<PetPlacingErrorOut> {
    override val name = "PetPlacingErrorOut"

    override fun read(r: PacketReader): PetPlacingErrorOut = readPetPlacingErrorOut(r)

    override fun write(w: PacketWriter, m: PetPlacingErrorOut) = writePetPlacingErrorOut(w, m)

    private fun readPetPlacingErrorOut(r: PacketReader): PetPlacingErrorOut {
        val vInt1 = r.int()
        return PetPlacingErrorOut(vInt1)
    }

    private fun writePetPlacingErrorOut(w: PacketWriter, m: PetPlacingErrorOut) {
        w.int(m.int1)
    }
}
