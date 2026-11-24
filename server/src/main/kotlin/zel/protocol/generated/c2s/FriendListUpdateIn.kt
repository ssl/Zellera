package zel.protocol.generated

data object FriendListUpdateIn

object FriendListUpdateInCodec : IncomingCodec<FriendListUpdateIn> {
    override val name = "FriendListUpdateIn"

    override fun read(r: PacketReader): FriendListUpdateIn = FriendListUpdateIn

    override fun write(w: PacketWriter, m: FriendListUpdateIn) = writeFriendListUpdateIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeFriendListUpdateIn(w: PacketWriter, m: FriendListUpdateIn) {
    }
}
