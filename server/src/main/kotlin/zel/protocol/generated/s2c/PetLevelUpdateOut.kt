package zel.protocol.generated

data class PetLevelUpdateOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object PetLevelUpdateOutCodec : OutgoingCodec<PetLevelUpdateOut> {
    override val name = "PetLevelUpdateOut"

    override fun read(r: PacketReader): PetLevelUpdateOut = readPetLevelUpdateOut(r)

    override fun write(w: PacketWriter, m: PetLevelUpdateOut) = writePetLevelUpdateOut(w, m)

    private fun readPetLevelUpdateOut(r: PacketReader): PetLevelUpdateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetLevelUpdateOut(vInt1, vInt2, vInt3)
    }

    private fun writePetLevelUpdateOut(w: PacketWriter, m: PetLevelUpdateOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
