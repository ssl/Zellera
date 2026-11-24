package zel.protocol.generated

data class PetExperienceOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object PetExperienceOutCodec : OutgoingCodec<PetExperienceOut> {
    override val name = "PetExperienceOut"

    override fun read(r: PacketReader): PetExperienceOut = readPetExperienceOut(r)

    override fun write(w: PacketWriter, m: PetExperienceOut) = writePetExperienceOut(w, m)

    private fun readPetExperienceOut(r: PacketReader): PetExperienceOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetExperienceOut(vInt1, vInt2, vInt3)
    }

    private fun writePetExperienceOut(w: PacketWriter, m: PetExperienceOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
