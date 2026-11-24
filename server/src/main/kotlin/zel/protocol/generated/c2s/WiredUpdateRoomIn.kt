package zel.protocol.generated

data class WiredUpdateRoomIn(
    val rollback: Boolean,
)

object WiredUpdateRoomInCodec : IncomingCodec<WiredUpdateRoomIn> {
    override val name = "WiredUpdateRoomIn"

    override fun read(r: PacketReader): WiredUpdateRoomIn = readWiredUpdateRoomIn(r)

    override fun write(w: PacketWriter, m: WiredUpdateRoomIn) = writeWiredUpdateRoomIn(w, m)

    private fun readWiredUpdateRoomIn(r: PacketReader): WiredUpdateRoomIn {
        val vRollback = r.boolean()
        return WiredUpdateRoomIn(vRollback)
    }

    private fun writeWiredUpdateRoomIn(w: PacketWriter, m: WiredUpdateRoomIn) {
        w.boolean(m.rollback)
    }
}
