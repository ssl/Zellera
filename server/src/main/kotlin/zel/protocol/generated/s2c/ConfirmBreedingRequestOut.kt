package zel.protocol.generated

data class ConfirmBreedingRequestOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val string2: String,
    val string3: String,
    val int4: Int,
    val string4: String,
    val int5: Int,
    val string5: String,
    val string6: String,
    val list1: List<List1Item>,
    val int6: Int,
) {
    data class List1Item(
        val int1: Int,
        val list1: List<Int>,
    )
}

object ConfirmBreedingRequestOutCodec : OutgoingCodec<ConfirmBreedingRequestOut> {
    override val name = "ConfirmBreedingRequestOut"

    override fun read(r: PacketReader): ConfirmBreedingRequestOut = readConfirmBreedingRequestOut(r)

    override fun write(w: PacketWriter, m: ConfirmBreedingRequestOut) = writeConfirmBreedingRequestOut(w, m)

    private fun readConfirmBreedingRequestOutList1Item(r: PacketReader): ConfirmBreedingRequestOut.List1Item {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        return ConfirmBreedingRequestOut.List1Item(vInt1, vList1)
    }

    private fun readConfirmBreedingRequestOut(r: PacketReader): ConfirmBreedingRequestOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt4 = r.int()
        val vString4 = r.string()
        val vInt5 = r.int()
        val vString5 = r.string()
        val vString6 = r.string()
        val vList1 = List(r.countInt(8)) { readConfirmBreedingRequestOutList1Item(r) }
        val vInt6 = r.int()
        return ConfirmBreedingRequestOut(vInt1, vInt2, vString1, vInt3, vString2, vString3, vInt4, vString4, vInt5, vString5, vString6, vList1, vInt6)
    }

    private fun writeConfirmBreedingRequestOutList1Item(w: PacketWriter, m: ConfirmBreedingRequestOut.List1Item) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }

    private fun writeConfirmBreedingRequestOut(w: PacketWriter, m: ConfirmBreedingRequestOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int4)
        w.string(m.string4)
        w.int(m.int5)
        w.string(m.string5)
        w.string(m.string6)
        w.countInt(m.list1.size)
        m.list1.forEach { writeConfirmBreedingRequestOutList1Item(w, it) }
        w.int(m.int6)
    }
}
