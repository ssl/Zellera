package zel.protocol.generated

data class WiredLinkConfigurationOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val boolean1: Boolean,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val int1: Int,
        val boolean1: Boolean,
    )
}

object WiredLinkConfigurationOutCodec : OutgoingCodec<WiredLinkConfigurationOut> {
    override val name = "WiredLinkConfigurationOut"

    override fun read(r: PacketReader): WiredLinkConfigurationOut = readWiredLinkConfigurationOut(r)

    override fun write(w: PacketWriter, m: WiredLinkConfigurationOut) = writeWiredLinkConfigurationOut(w, m)

    private fun readWiredLinkConfigurationOutList1Item(r: PacketReader): WiredLinkConfigurationOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return WiredLinkConfigurationOut.List1Item(vString1, vString2, vInt1, vBoolean1)
    }

    private fun readWiredLinkConfigurationOut(r: PacketReader): WiredLinkConfigurationOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vList1 = List(r.countInt(9)) { readWiredLinkConfigurationOutList1Item(r) }
        return WiredLinkConfigurationOut(vInt1, vInt2, vString1, vBoolean1, vList1)
    }

    private fun writeWiredLinkConfigurationOutList1Item(w: PacketWriter, m: WiredLinkConfigurationOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
        w.boolean(m.boolean1)
    }

    private fun writeWiredLinkConfigurationOut(w: PacketWriter, m: WiredLinkConfigurationOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredLinkConfigurationOutList1Item(w, it) }
    }
}
