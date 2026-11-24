package zel.protocol.generated

data class FlatControllersOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object FlatControllersOutCodec : OutgoingCodec<FlatControllersOut> {
    override val name = "FlatControllersOut"

    override fun read(r: PacketReader): FlatControllersOut = readFlatControllersOut(r)

    override fun write(w: PacketWriter, m: FlatControllersOut) = writeFlatControllersOut(w, m)

    private fun readFlatControllersOutList1Item(r: PacketReader): FlatControllersOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FlatControllersOut.List1Item(vInt1, vString1)
    }

    private fun readFlatControllersOut(r: PacketReader): FlatControllersOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(6)) { readFlatControllersOutList1Item(r) }
        return FlatControllersOut(vInt1, vList1)
    }

    private fun writeFlatControllersOutList1Item(w: PacketWriter, m: FlatControllersOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeFlatControllersOut(w: PacketWriter, m: FlatControllersOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeFlatControllersOutList1Item(w, it) }
    }
}
