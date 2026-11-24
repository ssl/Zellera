package zel.protocol.generated

data class WiredErrorLogsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val int2: Int,
        val long1: Long,
    )
}

object WiredErrorLogsOutCodec : OutgoingCodec<WiredErrorLogsOut> {
    override val name = "WiredErrorLogsOut"

    override fun read(r: PacketReader): WiredErrorLogsOut = readWiredErrorLogsOut(r)

    override fun write(w: PacketWriter, m: WiredErrorLogsOut) = writeWiredErrorLogsOut(w, m)

    private fun readWiredErrorLogsOutList1Item(r: PacketReader): WiredErrorLogsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vLong1 = r.long()
        return WiredErrorLogsOut.List1Item(vInt1, vString1, vString2, vInt2, vLong1)
    }

    private fun readWiredErrorLogsOut(r: PacketReader): WiredErrorLogsOut {
        val vList1 = List(r.countInt(20)) { readWiredErrorLogsOutList1Item(r) }
        return WiredErrorLogsOut(vList1)
    }

    private fun writeWiredErrorLogsOutList1Item(w: PacketWriter, m: WiredErrorLogsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int2)
        w.long(m.long1)
    }

    private fun writeWiredErrorLogsOut(w: PacketWriter, m: WiredErrorLogsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredErrorLogsOutList1Item(w, it) }
    }
}
