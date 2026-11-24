package zel.protocol.generated

data class AddFavouriteRoomIn(
    val roomId: Int,
)

object AddFavouriteRoomInCodec : IncomingCodec<AddFavouriteRoomIn> {
    override val name = "AddFavouriteRoomIn"

    override fun read(r: PacketReader): AddFavouriteRoomIn = readAddFavouriteRoomIn(r)

    override fun write(w: PacketWriter, m: AddFavouriteRoomIn) = writeAddFavouriteRoomIn(w, m)

    private fun readAddFavouriteRoomIn(r: PacketReader): AddFavouriteRoomIn {
        val vRoomId = r.int()
        return AddFavouriteRoomIn(vRoomId)
    }

    private fun writeAddFavouriteRoomIn(w: PacketWriter, m: AddFavouriteRoomIn) {
        w.int(m.roomId)
    }
}
