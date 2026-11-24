package zel.protocol.generated

data class FollowFriendIn(
    val int1: Int,
)

object FollowFriendInCodec : IncomingCodec<FollowFriendIn> {
    override val name = "FollowFriendIn"

    override fun read(r: PacketReader): FollowFriendIn = readFollowFriendIn(r)

    override fun write(w: PacketWriter, m: FollowFriendIn) = writeFollowFriendIn(w, m)

    private fun readFollowFriendIn(r: PacketReader): FollowFriendIn {
        val vInt1 = r.int()
        return FollowFriendIn(vInt1)
    }

    private fun writeFollowFriendIn(w: PacketWriter, m: FollowFriendIn) {
        w.int(m.int1)
    }
}
