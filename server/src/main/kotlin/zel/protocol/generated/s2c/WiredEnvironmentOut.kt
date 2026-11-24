package zel.protocol.generated

data class WiredEnvironmentOut(
    val boolean1: Boolean,
    val trailing1: Trailing1?,
) {
    data class Trailing1(
        val list1: List<String>,
    )
}

object WiredEnvironmentOutCodec : OutgoingCodec<WiredEnvironmentOut> {
    override val name = "WiredEnvironmentOut"

    override fun read(r: PacketReader): WiredEnvironmentOut = readWiredEnvironmentOut(r)

    override fun write(w: PacketWriter, m: WiredEnvironmentOut) = writeWiredEnvironmentOut(w, m)

    private fun readWiredEnvironmentOutTrailing1(r: PacketReader): WiredEnvironmentOut.Trailing1 {
        val vList1 = List(r.countInt(2)) { r.string() }
        return WiredEnvironmentOut.Trailing1(vList1)
    }

    private fun readWiredEnvironmentOut(r: PacketReader): WiredEnvironmentOut {
        val vBoolean1 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) readWiredEnvironmentOutTrailing1(r) else null
        return WiredEnvironmentOut(vBoolean1, vTrailing1)
    }

    private fun writeWiredEnvironmentOutTrailing1(w: PacketWriter, m: WiredEnvironmentOut.Trailing1) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }

    private fun writeWiredEnvironmentOut(w: PacketWriter, m: WiredEnvironmentOut) {
        w.boolean(m.boolean1)
        m.trailing1?.let { writeWiredEnvironmentOutTrailing1(w, it) }
    }
}
