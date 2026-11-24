package zel.protocol.generated

data class WiredGetVariablesForObjectIn(
    val type: Int,
    val id: Int,
)

object WiredGetVariablesForObjectInCodec : IncomingCodec<WiredGetVariablesForObjectIn> {
    override val name = "WiredGetVariablesForObjectIn"

    override fun read(r: PacketReader): WiredGetVariablesForObjectIn = readWiredGetVariablesForObjectIn(r)

    override fun write(w: PacketWriter, m: WiredGetVariablesForObjectIn) = writeWiredGetVariablesForObjectIn(w, m)

    private fun readWiredGetVariablesForObjectIn(r: PacketReader): WiredGetVariablesForObjectIn {
        val vType = r.int()
        val vId = r.int()
        return WiredGetVariablesForObjectIn(vType, vId)
    }

    private fun writeWiredGetVariablesForObjectIn(w: PacketWriter, m: WiredGetVariablesForObjectIn) {
        w.int(m.type)
        w.int(m.id)
    }
}
