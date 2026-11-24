package zel.protocol.generated

data class WardrobeOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
    )
}

object WardrobeOutCodec : OutgoingCodec<WardrobeOut> {
    override val name = "WardrobeOut"

    override fun read(r: PacketReader): WardrobeOut = readWardrobeOut(r)

    override fun write(w: PacketWriter, m: WardrobeOut) = writeWardrobeOut(w, m)

    private fun readWardrobeOutList1Item(r: PacketReader): WardrobeOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return WardrobeOut.List1Item(vInt1, vString1, vString2)
    }

    private fun readWardrobeOut(r: PacketReader): WardrobeOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(8)) { readWardrobeOutList1Item(r) }
        return WardrobeOut(vInt1, vList1)
    }

    private fun writeWardrobeOutList1Item(w: PacketWriter, m: WardrobeOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeWardrobeOut(w: PacketWriter, m: WardrobeOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWardrobeOutList1Item(w, it) }
    }
}
