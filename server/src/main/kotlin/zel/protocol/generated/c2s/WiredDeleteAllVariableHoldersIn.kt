package zel.protocol.generated

data class WiredDeleteAllVariableHoldersIn(
    val variableId: String,
)

object WiredDeleteAllVariableHoldersInCodec : IncomingCodec<WiredDeleteAllVariableHoldersIn> {
    override val name = "WiredDeleteAllVariableHoldersIn"

    override fun read(r: PacketReader): WiredDeleteAllVariableHoldersIn = readWiredDeleteAllVariableHoldersIn(r)

    override fun write(w: PacketWriter, m: WiredDeleteAllVariableHoldersIn) = writeWiredDeleteAllVariableHoldersIn(w, m)

    private fun readWiredDeleteAllVariableHoldersIn(r: PacketReader): WiredDeleteAllVariableHoldersIn {
        val vVariableId = r.string()
        return WiredDeleteAllVariableHoldersIn(vVariableId)
    }

    private fun writeWiredDeleteAllVariableHoldersIn(w: PacketWriter, m: WiredDeleteAllVariableHoldersIn) {
        w.string(m.variableId)
    }
}
