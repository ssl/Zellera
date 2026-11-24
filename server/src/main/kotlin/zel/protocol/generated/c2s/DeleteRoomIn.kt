package zel.protocol.generated

data class DeleteRoomIn(
    val roomId: Int,
)

object DeleteRoomInCodec : IncomingCodec<DeleteRoomIn> {
    override val name = "DeleteRoomIn"

    override fun read(r: PacketReader): DeleteRoomIn = readDeleteRoomIn(r)

    override fun write(w: PacketWriter, m: DeleteRoomIn) = writeDeleteRoomIn(w, m)

    private fun readDeleteRoomIn(r: PacketReader): DeleteRoomIn {
        val vRoomId = r.int()
        return DeleteRoomIn(vRoomId)
    }

    private fun writeDeleteRoomIn(w: PacketWriter, m: DeleteRoomIn) {
        w.int(m.roomId)
    }
}
