package zel.protocol.generated

data class WiredFurniHighlightOut(
    val list1: List<Int>,
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
)

object WiredFurniHighlightOutCodec : OutgoingCodec<WiredFurniHighlightOut> {
    override val name = "WiredFurniHighlightOut"

    override fun read(r: PacketReader): WiredFurniHighlightOut = readWiredFurniHighlightOut(r)

    override fun write(w: PacketWriter, m: WiredFurniHighlightOut) = writeWiredFurniHighlightOut(w, m)

    private fun readWiredFurniHighlightOut(r: PacketReader): WiredFurniHighlightOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        return WiredFurniHighlightOut(vList1, vInt1, vInt2, vInt3, vInt4, vInt5)
    }

    private fun writeWiredFurniHighlightOut(w: PacketWriter, m: WiredFurniHighlightOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
    }
}
