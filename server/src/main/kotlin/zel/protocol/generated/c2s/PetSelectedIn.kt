package zel.protocol.generated

data class PetSelectedIn(
    val petId: Int,
)

object PetSelectedInCodec : IncomingCodec<PetSelectedIn> {
    override val name = "PetSelectedIn"

    override fun read(r: PacketReader): PetSelectedIn = readPetSelectedIn(r)

    override fun write(w: PacketWriter, m: PetSelectedIn) = writePetSelectedIn(w, m)

    private fun readPetSelectedIn(r: PacketReader): PetSelectedIn {
        val vPetId = r.int()
        return PetSelectedIn(vPetId)
    }

    private fun writePetSelectedIn(w: PacketWriter, m: PetSelectedIn) {
        w.int(m.petId)
    }
}
