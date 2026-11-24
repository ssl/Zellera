package zel.protocol.generated

data class PetBreedingResultOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val string2: String,
    val int4: Int,
    val boolean1: Boolean,
    val int5: Int,
    val int6: Int,
    val string3: String,
    val int7: Int,
    val string4: String,
    val int8: Int,
    val boolean2: Boolean,
)

object PetBreedingResultOutCodec : OutgoingCodec<PetBreedingResultOut> {
    override val name = "PetBreedingResultOut"

    override fun read(r: PacketReader): PetBreedingResultOut = readPetBreedingResultOut(r)

    override fun write(w: PacketWriter, m: PetBreedingResultOut) = writePetBreedingResultOut(w, m)

    private fun readPetBreedingResultOut(r: PacketReader): PetBreedingResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vString2 = r.string()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vString3 = r.string()
        val vInt7 = r.int()
        val vString4 = r.string()
        val vInt8 = r.int()
        val vBoolean2 = r.boolean()
        return PetBreedingResultOut(vInt1, vInt2, vString1, vInt3, vString2, vInt4, vBoolean1, vInt5, vInt6, vString3, vInt7, vString4, vInt8, vBoolean2)
    }

    private fun writePetBreedingResultOut(w: PacketWriter, m: PetBreedingResultOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.string(m.string2)
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.int(m.int5)
        w.int(m.int6)
        w.string(m.string3)
        w.int(m.int7)
        w.string(m.string4)
        w.int(m.int8)
        w.boolean(m.boolean2)
    }
}
