package zel.protocol.generated

data class WiredLinkConnectionsOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
        val boolean1: Boolean,
        val boolean2: Boolean,
    )
}

object WiredLinkConnectionsOutCodec : OutgoingCodec<WiredLinkConnectionsOut> {
    override val name = "WiredLinkConnectionsOut"

    override fun read(r: PacketReader): WiredLinkConnectionsOut = readWiredLinkConnectionsOut(r)

    override fun write(w: PacketWriter, m: WiredLinkConnectionsOut) = writeWiredLinkConnectionsOut(w, m)

    private fun readWiredLinkConnectionsOutList1Item(r: PacketReader): WiredLinkConnectionsOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return WiredLinkConnectionsOut.List1Item(vInt1, vString1, vInt2, vBoolean1, vBoolean2)
    }

    private fun readWiredLinkConnectionsOut(r: PacketReader): WiredLinkConnectionsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vList1 = List(r.countInt(12)) { readWiredLinkConnectionsOutList1Item(r) }
        return WiredLinkConnectionsOut(vInt1, vInt2, vString1, vList1)
    }

    private fun writeWiredLinkConnectionsOutList1Item(w: PacketWriter, m: WiredLinkConnectionsOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }

    private fun writeWiredLinkConnectionsOut(w: PacketWriter, m: WiredLinkConnectionsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredLinkConnectionsOutList1Item(w, it) }
    }
}
