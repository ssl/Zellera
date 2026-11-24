package zel.protocol.generated

data class RemoveOwnRoomRightsRoomIn(
    val roomId: Int,
)

object RemoveOwnRoomRightsRoomInCodec : IncomingCodec<RemoveOwnRoomRightsRoomIn> {
    override val name = "RemoveOwnRoomRightsRoomIn"

    override fun read(r: PacketReader): RemoveOwnRoomRightsRoomIn = readRemoveOwnRoomRightsRoomIn(r)

    override fun write(w: PacketWriter, m: RemoveOwnRoomRightsRoomIn) = writeRemoveOwnRoomRightsRoomIn(w, m)

    private fun readRemoveOwnRoomRightsRoomIn(r: PacketReader): RemoveOwnRoomRightsRoomIn {
        val vRoomId = r.int()
        return RemoveOwnRoomRightsRoomIn(vRoomId)
    }

    private fun writeRemoveOwnRoomRightsRoomIn(w: PacketWriter, m: RemoveOwnRoomRightsRoomIn) {
        w.int(m.roomId)
    }
}
