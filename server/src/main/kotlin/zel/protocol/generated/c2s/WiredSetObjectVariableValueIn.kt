package zel.protocol.generated

data class WiredSetObjectVariableValueIn(
    val airTarget: Int,
    val objectId: Int,
    val variableId: String,
    val int1: Int,
    val action: Int,
)

object WiredSetObjectVariableValueInCodec : IncomingCodec<WiredSetObjectVariableValueIn> {
    override val name = "WiredSetObjectVariableValueIn"

    override fun read(r: PacketReader): WiredSetObjectVariableValueIn = readWiredSetObjectVariableValueIn(r)

    override fun write(w: PacketWriter, m: WiredSetObjectVariableValueIn) = writeWiredSetObjectVariableValueIn(w, m)

    private fun readWiredSetObjectVariableValueIn(r: PacketReader): WiredSetObjectVariableValueIn {
        val vAirTarget = r.int()
        val vObjectId = r.int()
        val vVariableId = r.string()
        val vInt1 = r.int()
        val vAction = r.int()
        return WiredSetObjectVariableValueIn(vAirTarget, vObjectId, vVariableId, vInt1, vAction)
    }

    private fun writeWiredSetObjectVariableValueIn(w: PacketWriter, m: WiredSetObjectVariableValueIn) {
        w.int(m.airTarget)
        w.int(m.objectId)
        w.string(m.variableId)
        w.int(m.int1)
        w.int(m.action)
    }
}
