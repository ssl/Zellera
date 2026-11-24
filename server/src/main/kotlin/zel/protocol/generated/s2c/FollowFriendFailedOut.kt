package zel.protocol.generated

data class FollowFriendFailedOut(
    val int1: Int,
)

object FollowFriendFailedOutCodec : OutgoingCodec<FollowFriendFailedOut> {
    override val name = "FollowFriendFailedOut"

    override fun read(r: PacketReader): FollowFriendFailedOut = readFollowFriendFailedOut(r)

    override fun write(w: PacketWriter, m: FollowFriendFailedOut) = writeFollowFriendFailedOut(w, m)

    private fun readFollowFriendFailedOut(r: PacketReader): FollowFriendFailedOut {
        val vInt1 = r.int()
        return FollowFriendFailedOut(vInt1)
    }

    private fun writeFollowFriendFailedOut(w: PacketWriter, m: FollowFriendFailedOut) {
        w.int(m.int1)
    }
}
