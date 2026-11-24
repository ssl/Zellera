package zel.protocol.generated

data class PetFigureUpdateOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val string1: String,
    val int5: Int,
    val list1: List<List1Item>,
    val boolean1: Boolean,
    val boolean2: Boolean,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
    )
}

object PetFigureUpdateOutCodec : OutgoingCodec<PetFigureUpdateOut> {
    override val name = "PetFigureUpdateOut"

    override fun read(r: PacketReader): PetFigureUpdateOut = readPetFigureUpdateOut(r)

    override fun write(w: PacketWriter, m: PetFigureUpdateOut) = writePetFigureUpdateOut(w, m)

    private fun readPetFigureUpdateOutList1Item(r: PacketReader): PetFigureUpdateOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return PetFigureUpdateOut.List1Item(vInt1, vInt2, vInt3)
    }

    private fun readPetFigureUpdateOut(r: PacketReader): PetFigureUpdateOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vString1 = r.string()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(12)) { readPetFigureUpdateOutList1Item(r) }
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return PetFigureUpdateOut(vInt1, vInt2, vInt3, vInt4, vString1, vInt5, vList1, vBoolean1, vBoolean2)
    }

    private fun writePetFigureUpdateOutList1Item(w: PacketWriter, m: PetFigureUpdateOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writePetFigureUpdateOut(w: PacketWriter, m: PetFigureUpdateOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.string(m.string1)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writePetFigureUpdateOutList1Item(w, it) }
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }
}
