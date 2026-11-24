package zel.protocol.generated

data class CanCreateRoomOut(
    val resultCode: Int,
    val roomLimit: Int,
)

object CanCreateRoomOutCodec : OutgoingCodec<CanCreateRoomOut> {
    override val name = "CanCreateRoomOut"

    override fun read(r: PacketReader): CanCreateRoomOut = readCanCreateRoomOut(r)

    override fun write(w: PacketWriter, m: CanCreateRoomOut) = writeCanCreateRoomOut(w, m)

    private fun readCanCreateRoomOut(r: PacketReader): CanCreateRoomOut {
        val vResultCode = r.int()
        val vRoomLimit = r.int()
        return CanCreateRoomOut(vResultCode, vRoomLimit)
    }

    private fun writeCanCreateRoomOut(w: PacketWriter, m: CanCreateRoomOut) {
        w.int(m.resultCode)
        w.int(m.roomLimit)
    }
}
