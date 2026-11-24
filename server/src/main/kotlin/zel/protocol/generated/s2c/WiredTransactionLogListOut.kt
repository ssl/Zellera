package zel.protocol.generated

data class WiredTransactionLogListOut(
    val int1: Int,
    val long1: Long,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
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
    )
}

object WiredTransactionLogListOutCodec : OutgoingCodec<WiredTransactionLogListOut> {
    override val name = "WiredTransactionLogListOut"

    override fun read(r: PacketReader): WiredTransactionLogListOut = readWiredTransactionLogListOut(r)

    override fun write(w: PacketWriter, m: WiredTransactionLogListOut) = writeWiredTransactionLogListOut(w, m)

    private fun readWiredTransactionLogListOutList1Item(r: PacketReader): WiredTransactionLogListOut.List1Item {
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
        return WiredTransactionLogListOut.List1Item(vLong1, vInt1, vInt2, vString1, vInt3, vString2, vLong2, vString3, vInt4, vInt5, vInt6, vInt7, vInt8)
    }

    private fun readWiredTransactionLogListOut(r: PacketReader): WiredTransactionLogListOut {
        val vInt1 = r.int()
        val vLong1 = r.long()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vList1 = List(r.countInt(54)) { readWiredTransactionLogListOutList1Item(r) }
        return WiredTransactionLogListOut(vInt1, vLong1, vInt2, vInt3, vInt4, vList1)
    }

    private fun writeWiredTransactionLogListOutList1Item(w: PacketWriter, m: WiredTransactionLogListOut.List1Item) {
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
    }

    private fun writeWiredTransactionLogListOut(w: PacketWriter, m: WiredTransactionLogListOut) {
        w.int(m.int1)
        w.long(m.long1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredTransactionLogListOutList1Item(w, it) }
    }
}
