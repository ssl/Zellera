package zel.protocol.generated

data class PlayListOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val string1: String,
        val string2: String,
    )
}

object PlayListOutCodec : OutgoingCodec<PlayListOut> {
    override val name = "PlayListOut"

    override fun read(r: PacketReader): PlayListOut = readPlayListOut(r)

    override fun write(w: PacketWriter, m: PlayListOut) = writePlayListOut(w, m)

    private fun readPlayListOutList1Item(r: PacketReader): PlayListOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return PlayListOut.List1Item(vInt1, vInt2, vString1, vString2)
    }

    private fun readPlayListOut(r: PacketReader): PlayListOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(12)) { readPlayListOutList1Item(r) }
        return PlayListOut(vInt1, vList1)
    }

    private fun writePlayListOutList1Item(w: PacketWriter, m: PlayListOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writePlayListOut(w: PacketWriter, m: PlayListOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayListOutList1Item(w, it) }
    }
}
