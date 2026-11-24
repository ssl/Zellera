package zel.protocol.generated

data class HeightMapOut(
    val int1: Int,
    val list1: List<Short>,
)

object HeightMapOutCodec : OutgoingCodec<HeightMapOut> {
    override val name = "HeightMapOut"

    override fun read(r: PacketReader): HeightMapOut = readHeightMapOut(r)

    override fun write(w: PacketWriter, m: HeightMapOut) = writeHeightMapOut(w, m)

    private fun readHeightMapOut(r: PacketReader): HeightMapOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(2)) { r.short() }
        return HeightMapOut(vInt1, vList1)
    }

    private fun writeHeightMapOut(w: PacketWriter, m: HeightMapOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.short(it) }
    }
}
