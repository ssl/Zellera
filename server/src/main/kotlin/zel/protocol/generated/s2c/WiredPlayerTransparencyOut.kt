package zel.protocol.generated

data class WiredPlayerTransparencyOut(
    val list1: List<Int>,
    val int1: Int,
    val boolean1: Boolean,
    val int2: Int,
)

object WiredPlayerTransparencyOutCodec : OutgoingCodec<WiredPlayerTransparencyOut> {
    override val name = "WiredPlayerTransparencyOut"

    override fun read(r: PacketReader): WiredPlayerTransparencyOut = readWiredPlayerTransparencyOut(r)

    override fun write(w: PacketWriter, m: WiredPlayerTransparencyOut) = writeWiredPlayerTransparencyOut(w, m)

    private fun readWiredPlayerTransparencyOut(r: PacketReader): WiredPlayerTransparencyOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        return WiredPlayerTransparencyOut(vList1, vInt1, vBoolean1, vInt2)
    }

    private fun writeWiredPlayerTransparencyOut(w: PacketWriter, m: WiredPlayerTransparencyOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
    }
}
