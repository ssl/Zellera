package zel.protocol.generated

data class HeightMapUpdateOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val byte1: Byte,
        val byte2: Byte,
        val short1: Short,
    )
}

object HeightMapUpdateOutCodec : OutgoingCodec<HeightMapUpdateOut> {
    override val name = "HeightMapUpdateOut"

    override fun read(r: PacketReader): HeightMapUpdateOut = readHeightMapUpdateOut(r)

    override fun write(w: PacketWriter, m: HeightMapUpdateOut) = writeHeightMapUpdateOut(w, m)

    private fun readHeightMapUpdateOutList1Item(r: PacketReader): HeightMapUpdateOut.List1Item {
        val vByte1 = r.byte()
        val vByte2 = r.byte()
        val vShort1 = r.short()
        return HeightMapUpdateOut.List1Item(vByte1, vByte2, vShort1)
    }

    private fun readHeightMapUpdateOut(r: PacketReader): HeightMapUpdateOut {
        val vList1 = List(r.countByte(4)) { readHeightMapUpdateOutList1Item(r) }
        return HeightMapUpdateOut(vList1)
    }

    private fun writeHeightMapUpdateOutList1Item(w: PacketWriter, m: HeightMapUpdateOut.List1Item) {
        w.byte(m.byte1)
        w.byte(m.byte2)
        w.short(m.short1)
    }

    private fun writeHeightMapUpdateOut(w: PacketWriter, m: HeightMapUpdateOut) {
        w.countByte(m.list1.size)
        m.list1.forEach { writeHeightMapUpdateOutList1Item(w, it) }
    }
}
