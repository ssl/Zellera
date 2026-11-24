package zel.protocol.generated

data object RoomsWhereMyFriendsAreSearchIn

object RoomsWhereMyFriendsAreSearchInCodec : IncomingCodec<RoomsWhereMyFriendsAreSearchIn> {
    override val name = "RoomsWhereMyFriendsAreSearchIn"

    override fun read(r: PacketReader): RoomsWhereMyFriendsAreSearchIn = RoomsWhereMyFriendsAreSearchIn

    override fun write(w: PacketWriter, m: RoomsWhereMyFriendsAreSearchIn) = writeRoomsWhereMyFriendsAreSearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRoomsWhereMyFriendsAreSearchIn(w: PacketWriter, m: RoomsWhereMyFriendsAreSearchIn) {
    }
}
