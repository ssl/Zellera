package zel.protocol.generated

data class PetBreedingOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object PetBreedingOutCodec : OutgoingCodec<PetBreedingOut> {
    override val name = "PetBreedingOut"

    override fun read(r: PacketReader): PetBreedingOut = readPetBreedingOut(r)

    override fun write(w: PacketWriter, m: PetBreedingOut) = writePetBreedingOut(w, m)

    private fun readPetBreedingOut(r: PacketReader): PetBreedingOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetBreedingOut(vInt1, vInt2, vInt3)
    }

    private fun writePetBreedingOut(w: PacketWriter, m: PetBreedingOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
