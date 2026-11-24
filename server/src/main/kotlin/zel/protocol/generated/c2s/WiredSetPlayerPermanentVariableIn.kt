package zel.protocol.generated

data class WiredSetPlayerPermanentVariableIn(
    val entityType: Int,
    val userId: Int,
    val variableId: String,
    val int1: Int,
    val action: Int,
)

object WiredSetPlayerPermanentVariableInCodec : IncomingCodec<WiredSetPlayerPermanentVariableIn> {
    override val name = "WiredSetPlayerPermanentVariableIn"

    override fun read(r: PacketReader): WiredSetPlayerPermanentVariableIn = readWiredSetPlayerPermanentVariableIn(r)

    override fun write(w: PacketWriter, m: WiredSetPlayerPermanentVariableIn) = writeWiredSetPlayerPermanentVariableIn(w, m)

    private fun readWiredSetPlayerPermanentVariableIn(r: PacketReader): WiredSetPlayerPermanentVariableIn {
        val vEntityType = r.int()
        val vUserId = r.int()
        val vVariableId = r.string()
        val vInt1 = r.int()
        val vAction = r.int()
        return WiredSetPlayerPermanentVariableIn(vEntityType, vUserId, vVariableId, vInt1, vAction)
    }

    private fun writeWiredSetPlayerPermanentVariableIn(w: PacketWriter, m: WiredSetPlayerPermanentVariableIn) {
        w.int(m.entityType)
        w.int(m.userId)
        w.string(m.variableId)
        w.int(m.int1)
        w.int(m.action)
    }
}
