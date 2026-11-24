package zel.protocol.generated

data class WiredAllVariablesDiffsOut(
    val int1: Int,
    val boolean1: Boolean,
    val list1: List<String>,
    val rest: ByteArray,
)

object WiredAllVariablesDiffsOutCodec : OutgoingCodec<WiredAllVariablesDiffsOut> {
    override val name = "WiredAllVariablesDiffsOut"

    override fun read(r: PacketReader): WiredAllVariablesDiffsOut = readWiredAllVariablesDiffsOut(r)

    override fun write(w: PacketWriter, m: WiredAllVariablesDiffsOut) = writeWiredAllVariablesDiffsOut(w, m)

    private fun readWiredAllVariablesDiffsOut(r: PacketReader): WiredAllVariablesDiffsOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vList1 = List(r.countInt(2)) { r.string() }
        val vRest = r.rest()
        return WiredAllVariablesDiffsOut(vInt1, vBoolean1, vList1, vRest)
    }

    private fun writeWiredAllVariablesDiffsOut(w: PacketWriter, m: WiredAllVariablesDiffsOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
        w.bytes(m.rest)
    }
}
