package zel.protocol.generated

data class WiredFurniTransparencyOut(
    val list1: List<Int>,
    val int1: Int,
    val boolean1: Boolean,
    val int2: Int,
)

object WiredFurniTransparencyOutCodec : OutgoingCodec<WiredFurniTransparencyOut> {
    override val name = "WiredFurniTransparencyOut"

    override fun read(r: PacketReader): WiredFurniTransparencyOut = readWiredFurniTransparencyOut(r)

    override fun write(w: PacketWriter, m: WiredFurniTransparencyOut) = writeWiredFurniTransparencyOut(w, m)

    private fun readWiredFurniTransparencyOut(r: PacketReader): WiredFurniTransparencyOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        return WiredFurniTransparencyOut(vList1, vInt1, vBoolean1, vInt2)
    }

    private fun writeWiredFurniTransparencyOut(w: PacketWriter, m: WiredFurniTransparencyOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
    }
}
