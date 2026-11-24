package zel.protocol.generated

data class DeleteFavouriteRoomIn(
    val roomId: Int,
)

object DeleteFavouriteRoomInCodec : IncomingCodec<DeleteFavouriteRoomIn> {
    override val name = "DeleteFavouriteRoomIn"

    override fun read(r: PacketReader): DeleteFavouriteRoomIn = readDeleteFavouriteRoomIn(r)

    override fun write(w: PacketWriter, m: DeleteFavouriteRoomIn) = writeDeleteFavouriteRoomIn(w, m)

    private fun readDeleteFavouriteRoomIn(r: PacketReader): DeleteFavouriteRoomIn {
        val vRoomId = r.int()
        return DeleteFavouriteRoomIn(vRoomId)
    }

    private fun writeDeleteFavouriteRoomIn(w: PacketWriter, m: DeleteFavouriteRoomIn) {
        w.int(m.roomId)
    }
}
