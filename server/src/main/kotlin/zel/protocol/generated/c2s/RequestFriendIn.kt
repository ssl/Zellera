package zel.protocol.generated

data class RequestFriendIn(
    val username: String,
)

object RequestFriendInCodec : IncomingCodec<RequestFriendIn> {
    override val name = "RequestFriendIn"

    override fun read(r: PacketReader): RequestFriendIn = readRequestFriendIn(r)

    override fun write(w: PacketWriter, m: RequestFriendIn) = writeRequestFriendIn(w, m)

    private fun readRequestFriendIn(r: PacketReader): RequestFriendIn {
        val vUsername = r.string()
        return RequestFriendIn(vUsername)
    }

    private fun writeRequestFriendIn(w: PacketWriter, m: RequestFriendIn) {
        w.string(m.username)
    }
}
