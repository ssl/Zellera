package zel.protocol.generated

data class PlayerIconsOut(
    val list1: List<List1Item>,
    val list2: List<Int>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
    )
}

object PlayerIconsOutCodec : OutgoingCodec<PlayerIconsOut> {
    override val name = "PlayerIconsOut"

    override fun read(r: PacketReader): PlayerIconsOut = readPlayerIconsOut(r)

    override fun write(w: PacketWriter, m: PlayerIconsOut) = writePlayerIconsOut(w, m)

    private fun readPlayerIconsOutList1Item(r: PacketReader): PlayerIconsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PlayerIconsOut.List1Item(vInt1, vInt2)
    }

    private fun readPlayerIconsOut(r: PacketReader): PlayerIconsOut {
        val vList1 = List(r.countInt(8)) { readPlayerIconsOutList1Item(r) }
        val vList2 = List(r.countInt(4)) { r.int() }
        return PlayerIconsOut(vList1, vList2)
    }

    private fun writePlayerIconsOutList1Item(w: PacketWriter, m: PlayerIconsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writePlayerIconsOut(w: PacketWriter, m: PlayerIconsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayerIconsOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.int(it) }
    }
}
