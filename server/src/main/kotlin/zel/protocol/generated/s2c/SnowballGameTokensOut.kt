package zel.protocol.generated

data class SnowballGameTokensOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val int2: Int,
        val int3: Int,
        val int4: Int,
    )
}

object SnowballGameTokensOutCodec : OutgoingCodec<SnowballGameTokensOut> {
    override val name = "SnowballGameTokensOut"

    override fun read(r: PacketReader): SnowballGameTokensOut = readSnowballGameTokensOut(r)

    override fun write(w: PacketWriter, m: SnowballGameTokensOut) = writeSnowballGameTokensOut(w, m)

    private fun readSnowballGameTokensOutList1Item(r: PacketReader): SnowballGameTokensOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return SnowballGameTokensOut.List1Item(vInt1, vString1, vInt2, vInt3, vInt4)
    }

    private fun readSnowballGameTokensOut(r: PacketReader): SnowballGameTokensOut {
        val vList1 = List(r.countInt(18)) { readSnowballGameTokensOutList1Item(r) }
        return SnowballGameTokensOut(vList1)
    }

    private fun writeSnowballGameTokensOutList1Item(w: PacketWriter, m: SnowballGameTokensOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }

    private fun writeSnowballGameTokensOut(w: PacketWriter, m: SnowballGameTokensOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeSnowballGameTokensOutList1Item(w, it) }
    }
}
