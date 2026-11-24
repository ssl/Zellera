package zel.protocol.generated

data class VariableFxConfigsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val boolean1: Boolean,
        val int2: Int,
        val int3: Int,
        val boolean2: Boolean,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val int7: Int,
        val int8: Int,
        val int9: Int,
        val long1: Long,
        val long2: Long,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val string1: String,
            val string2: String,
        )
    }
}

object VariableFxConfigsOutCodec : OutgoingCodec<VariableFxConfigsOut> {
    override val name = "VariableFxConfigsOut"

    override fun read(r: PacketReader): VariableFxConfigsOut = readVariableFxConfigsOut(r)

    override fun write(w: PacketWriter, m: VariableFxConfigsOut) = writeVariableFxConfigsOut(w, m)

    private fun readVariableFxConfigsOutList1ItemList1Item(r: PacketReader): VariableFxConfigsOut.List1Item.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return VariableFxConfigsOut.List1Item.List1Item(vString1, vString2)
    }

    private fun readVariableFxConfigsOutList1Item(r: PacketReader): VariableFxConfigsOut.List1Item {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vBoolean2 = r.boolean()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vLong1 = r.long()
        val vLong2 = r.long()
        val vList1 = List(r.countInt(4)) { readVariableFxConfigsOutList1ItemList1Item(r) }
        return VariableFxConfigsOut.List1Item(vInt1, vBoolean1, vInt2, vInt3, vBoolean2, vInt4, vInt5, vInt6, vInt7, vInt8, vInt9, vLong1, vLong2, vList1)
    }

    private fun readVariableFxConfigsOut(r: PacketReader): VariableFxConfigsOut {
        val vList1 = List(r.countInt(58)) { readVariableFxConfigsOutList1Item(r) }
        return VariableFxConfigsOut(vList1)
    }

    private fun writeVariableFxConfigsOutList1ItemList1Item(w: PacketWriter, m: VariableFxConfigsOut.List1Item.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeVariableFxConfigsOutList1Item(w: PacketWriter, m: VariableFxConfigsOut.List1Item) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.boolean(m.boolean2)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.long(m.long1)
        w.long(m.long2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeVariableFxConfigsOutList1ItemList1Item(w, it) }
    }

    private fun writeVariableFxConfigsOut(w: PacketWriter, m: VariableFxConfigsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeVariableFxConfigsOutList1Item(w, it) }
    }
}
