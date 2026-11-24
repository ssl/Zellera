package zel.protocol.generated

data class VariableFxStatusRemovedOut(
    val list1: List<String>,
)

object VariableFxStatusRemovedOutCodec : OutgoingCodec<VariableFxStatusRemovedOut> {
    override val name = "VariableFxStatusRemovedOut"

    override fun read(r: PacketReader): VariableFxStatusRemovedOut = readVariableFxStatusRemovedOut(r)

    override fun write(w: PacketWriter, m: VariableFxStatusRemovedOut) = writeVariableFxStatusRemovedOut(w, m)

    private fun readVariableFxStatusRemovedOut(r: PacketReader): VariableFxStatusRemovedOut {
        val vList1 = List(r.countInt(2)) { r.string() }
        return VariableFxStatusRemovedOut(vList1)
    }

    private fun writeVariableFxStatusRemovedOut(w: PacketWriter, m: VariableFxStatusRemovedOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
