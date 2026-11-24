package zel.protocol.generated

data class WiredAllVariablesHashOut(
    val int1: Int,
)

object WiredAllVariablesHashOutCodec : OutgoingCodec<WiredAllVariablesHashOut> {
    override val name = "WiredAllVariablesHashOut"

    override fun read(r: PacketReader): WiredAllVariablesHashOut = readWiredAllVariablesHashOut(r)

    override fun write(w: PacketWriter, m: WiredAllVariablesHashOut) = writeWiredAllVariablesHashOut(w, m)

    private fun readWiredAllVariablesHashOut(r: PacketReader): WiredAllVariablesHashOut {
        val vInt1 = r.int()
        return WiredAllVariablesHashOut(vInt1)
    }

    private fun writeWiredAllVariablesHashOut(w: PacketWriter, m: WiredAllVariablesHashOut) {
        w.int(m.int1)
    }
}
