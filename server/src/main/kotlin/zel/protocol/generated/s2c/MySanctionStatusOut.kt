package zel.protocol.generated

data class MySanctionStatusOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
        val int2: Int,
        val string2: String,
        val boolean1: Boolean,
        val int3: Int,
        val string3: String,
        val int4: Int,
        val int5: Int,
    )
}

object MySanctionStatusOutCodec : OutgoingCodec<MySanctionStatusOut> {
    override val name = "MySanctionStatusOut"

    override fun read(r: PacketReader): MySanctionStatusOut = readMySanctionStatusOut(r)

    override fun write(w: PacketWriter, m: MySanctionStatusOut) = writeMySanctionStatusOut(w, m)

    private fun readMySanctionStatusOutList1Item(r: PacketReader): MySanctionStatusOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        val vInt3 = r.int()
        val vString3 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        return MySanctionStatusOut.List1Item(vString1, vInt1, vInt2, vString2, vBoolean1, vInt3, vString3, vInt4, vInt5)
    }

    private fun readMySanctionStatusOut(r: PacketReader): MySanctionStatusOut {
        val vList1 = List(r.countInt(27)) { readMySanctionStatusOutList1Item(r) }
        return MySanctionStatusOut(vList1)
    }

    private fun writeMySanctionStatusOutList1Item(w: PacketWriter, m: MySanctionStatusOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string2)
        w.boolean(m.boolean1)
        w.int(m.int3)
        w.string(m.string3)
        w.int(m.int4)
        w.int(m.int5)
    }

    private fun writeMySanctionStatusOut(w: PacketWriter, m: MySanctionStatusOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeMySanctionStatusOutList1Item(w, it) }
    }
}
