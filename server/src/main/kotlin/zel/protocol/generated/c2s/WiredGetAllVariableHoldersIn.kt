package zel.protocol.generated

data class WiredGetAllVariableHoldersIn(
    val variableId: String,
)

object WiredGetAllVariableHoldersInCodec : IncomingCodec<WiredGetAllVariableHoldersIn> {
    override val name = "WiredGetAllVariableHoldersIn"

    override fun read(r: PacketReader): WiredGetAllVariableHoldersIn = readWiredGetAllVariableHoldersIn(r)

    override fun write(w: PacketWriter, m: WiredGetAllVariableHoldersIn) = writeWiredGetAllVariableHoldersIn(w, m)

    private fun readWiredGetAllVariableHoldersIn(r: PacketReader): WiredGetAllVariableHoldersIn {
        val vVariableId = r.string()
        return WiredGetAllVariableHoldersIn(vVariableId)
    }

    private fun writeWiredGetAllVariableHoldersIn(w: PacketWriter, m: WiredGetAllVariableHoldersIn) {
        w.string(m.variableId)
    }
}
