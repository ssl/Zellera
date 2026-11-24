package zel.protocol.generated

data object MyFavouriteRoomsSearchIn

object MyFavouriteRoomsSearchInCodec : IncomingCodec<MyFavouriteRoomsSearchIn> {
    override val name = "MyFavouriteRoomsSearchIn"

    override fun read(r: PacketReader): MyFavouriteRoomsSearchIn = MyFavouriteRoomsSearchIn

    override fun write(w: PacketWriter, m: MyFavouriteRoomsSearchIn) = writeMyFavouriteRoomsSearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyFavouriteRoomsSearchIn(w: PacketWriter, m: MyFavouriteRoomsSearchIn) {
    }
}
