package zel.protocol.generated

data class VariableFxConfigsRemovedOut(
    val list1: List<Int>,
)

object VariableFxConfigsRemovedOutCodec : OutgoingCodec<VariableFxConfigsRemovedOut> {
    override val name = "VariableFxConfigsRemovedOut"

    override fun read(r: PacketReader): VariableFxConfigsRemovedOut = readVariableFxConfigsRemovedOut(r)

    override fun write(w: PacketWriter, m: VariableFxConfigsRemovedOut) = writeVariableFxConfigsRemovedOut(w, m)

    private fun readVariableFxConfigsRemovedOut(r: PacketReader): VariableFxConfigsRemovedOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        return VariableFxConfigsRemovedOut(vList1)
    }

    private fun writeVariableFxConfigsRemovedOut(w: PacketWriter, m: VariableFxConfigsRemovedOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
