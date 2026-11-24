package zel.protocol.generated

data class WiredGetPlayerPermanentVariablesIn(
    val entityType: Int,
    val entityId: Int,
)

object WiredGetPlayerPermanentVariablesInCodec : IncomingCodec<WiredGetPlayerPermanentVariablesIn> {
    override val name = "WiredGetPlayerPermanentVariablesIn"

    override fun read(r: PacketReader): WiredGetPlayerPermanentVariablesIn = readWiredGetPlayerPermanentVariablesIn(r)

    override fun write(w: PacketWriter, m: WiredGetPlayerPermanentVariablesIn) = writeWiredGetPlayerPermanentVariablesIn(w, m)

    private fun readWiredGetPlayerPermanentVariablesIn(r: PacketReader): WiredGetPlayerPermanentVariablesIn {
        val vEntityType = r.int()
        val vEntityId = r.int()
        return WiredGetPlayerPermanentVariablesIn(vEntityType, vEntityId)
    }

    private fun writeWiredGetPlayerPermanentVariablesIn(w: PacketWriter, m: WiredGetPlayerPermanentVariablesIn) {
        w.int(m.entityType)
        w.int(m.entityId)
    }
}
