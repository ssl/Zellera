package zel.protocol.generated

data object GetFriendRequestsIn

object GetFriendRequestsInCodec : IncomingCodec<GetFriendRequestsIn> {
    override val name = "GetFriendRequestsIn"

    override fun read(r: PacketReader): GetFriendRequestsIn = GetFriendRequestsIn

    override fun write(w: PacketWriter, m: GetFriendRequestsIn) = writeGetFriendRequestsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetFriendRequestsIn(w: PacketWriter, m: GetFriendRequestsIn) {
    }
}
