package zel.protocol.generated

data class GetCustomFilterResultOut(
    val list1: List<String>,
)

object GetCustomFilterResultOutCodec : OutgoingCodec<GetCustomFilterResultOut> {
    override val name = "GetCustomFilterResultOut"

    override fun read(r: PacketReader): GetCustomFilterResultOut = readGetCustomFilterResultOut(r)

    override fun write(w: PacketWriter, m: GetCustomFilterResultOut) = writeGetCustomFilterResultOut(w, m)

    private fun readGetCustomFilterResultOut(r: PacketReader): GetCustomFilterResultOut {
        val vList1 = List(r.countInt(2)) { r.string() }
        return GetCustomFilterResultOut(vList1)
    }

    private fun writeGetCustomFilterResultOut(w: PacketWriter, m: GetCustomFilterResultOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
