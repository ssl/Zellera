package zel.protocol.generated

data class PetReceivedOut(
    val boolean1: Boolean,
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val string2: String,
    val int4: Int,
    val list1: List<List1Item>,
    val int5: Int,
    val int6: Int,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
    )
}

object PetReceivedOutCodec : OutgoingCodec<PetReceivedOut> {
    override val name = "PetReceivedOut"

    override fun read(r: PacketReader): PetReceivedOut = readPetReceivedOut(r)

    override fun write(w: PacketWriter, m: PetReceivedOut) = writePetReceivedOut(w, m)

    private fun readPetReceivedOutList1Item(r: PacketReader): PetReceivedOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetReceivedOut.List1Item(vInt1, vInt2, vInt3)
    }

    private fun readPetReceivedOut(r: PacketReader): PetReceivedOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString2 = r.string()
        val vInt4 = r.int()
        val vList1 = List(r.countInt(12)) { readPetReceivedOutList1Item(r) }
        val vInt5 = r.int()
        val vInt6 = r.int()
        return PetReceivedOut(vBoolean1, vInt1, vString1, vInt2, vInt3, vString2, vInt4, vList1, vInt5, vInt6)
    }

    private fun writePetReceivedOutList1Item(w: PacketWriter, m: PetReceivedOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writePetReceivedOut(w: PacketWriter, m: PetReceivedOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string2)
        w.int(m.int4)
        w.countInt(m.list1.size)
        m.list1.forEach { writePetReceivedOutList1Item(w, it) }
        w.int(m.int5)
        w.int(m.int6)
    }
}
