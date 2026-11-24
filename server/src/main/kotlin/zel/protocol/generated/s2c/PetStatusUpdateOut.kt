package zel.protocol.generated

data class PetStatusUpdateOut(
    val int1: Int,
    val int2: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val boolean4: Boolean,
)

object PetStatusUpdateOutCodec : OutgoingCodec<PetStatusUpdateOut> {
    override val name = "PetStatusUpdateOut"

    override fun read(r: PacketReader): PetStatusUpdateOut = readPetStatusUpdateOut(r)

    override fun write(w: PacketWriter, m: PetStatusUpdateOut) = writePetStatusUpdateOut(w, m)

    private fun readPetStatusUpdateOut(r: PacketReader): PetStatusUpdateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        return PetStatusUpdateOut(vInt1, vInt2, vBoolean1, vBoolean2, vBoolean3, vBoolean4)
    }

    private fun writePetStatusUpdateOut(w: PacketWriter, m: PetStatusUpdateOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
    }
}
