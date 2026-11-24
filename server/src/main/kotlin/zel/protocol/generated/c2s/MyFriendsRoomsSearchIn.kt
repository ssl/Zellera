package zel.protocol.generated

data object MyFriendsRoomsSearchIn

object MyFriendsRoomsSearchInCodec : IncomingCodec<MyFriendsRoomsSearchIn> {
    override val name = "MyFriendsRoomsSearchIn"

    override fun read(r: PacketReader): MyFriendsRoomsSearchIn = MyFriendsRoomsSearchIn

    override fun write(w: PacketWriter, m: MyFriendsRoomsSearchIn) = writeMyFriendsRoomsSearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyFriendsRoomsSearchIn(w: PacketWriter, m: MyFriendsRoomsSearchIn) {
    }
}
