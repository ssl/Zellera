package zel.protocol.generated

data class WiredTransactionLogDetailsOut(
    val long1: Long,
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
    val string2: String,
    val long2: Long,
    val string3: String,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val int8: Int,
    val list1: List<Int>,
    val list2: List<List2Item>,
    val list3: List<List3Item>,
    val boolean1: Boolean,
) {
    data class List2Item(
        val boolean1: Boolean,
        val int1: Int,
        val string1: String,
        val int2: Int,
    )

    data class List3Item(
        val boolean1: Boolean,
        val int1: Int,
        val string1: String,
        val int2: Int,
    )
}

object WiredTransactionLogDetailsOutCodec : OutgoingCodec<WiredTransactionLogDetailsOut> {
    override val name = "WiredTransactionLogDetailsOut"

    override fun read(r: PacketReader): WiredTransactionLogDetailsOut = readWiredTransactionLogDetailsOut(r)

    override fun write(w: PacketWriter, m: WiredTransactionLogDetailsOut) = writeWiredTransactionLogDetailsOut(w, m)

    private fun readWiredTransactionLogDetailsOutList2Item(r: PacketReader): WiredTransactionLogDetailsOut.List2Item {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return WiredTransactionLogDetailsOut.List2Item(vBoolean1, vInt1, vString1, vInt2)
    }

    private fun readWiredTransactionLogDetailsOutList3Item(r: PacketReader): WiredTransactionLogDetailsOut.List3Item {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return WiredTransactionLogDetailsOut.List3Item(vBoolean1, vInt1, vString1, vInt2)
    }

    private fun readWiredTransactionLogDetailsOut(r: PacketReader): WiredTransactionLogDetailsOut {
        val vLong1 = r.long()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vString2 = r.string()
        val vLong2 = r.long()
        val vString3 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(11)) { readWiredTransactionLogDetailsOutList2Item(r) }
        val vList3 = List(r.countInt(11)) { readWiredTransactionLogDetailsOutList3Item(r) }
        val vBoolean1 = r.boolean()
        return WiredTransactionLogDetailsOut(vLong1, vInt1, vInt2, vString1, vInt3, vString2, vLong2, vString3, vInt4, vInt5, vInt6, vInt7, vInt8, vList1, vList2, vList3, vBoolean1)
    }

    private fun writeWiredTransactionLogDetailsOutList2Item(w: PacketWriter, m: WiredTransactionLogDetailsOut.List2Item) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }

    private fun writeWiredTransactionLogDetailsOutList3Item(w: PacketWriter, m: WiredTransactionLogDetailsOut.List3Item) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }

    private fun writeWiredTransactionLogDetailsOut(w: PacketWriter, m: WiredTransactionLogDetailsOut) {
        w.long(m.long1)
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.string(m.string2)
        w.long(m.long2)
        w.string(m.string3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeWiredTransactionLogDetailsOutList2Item(w, it) }
        w.countInt(m.list3.size)
        m.list3.forEach { writeWiredTransactionLogDetailsOutList3Item(w, it) }
        w.boolean(m.boolean1)
    }
}
