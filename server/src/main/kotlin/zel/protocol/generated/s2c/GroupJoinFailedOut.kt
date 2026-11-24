package zel.protocol.generated

data class GroupJoinFailedOut(
    val int1: Int,
)

object GroupJoinFailedOutCodec : OutgoingCodec<GroupJoinFailedOut> {
    override val name = "GroupJoinFailedOut"

    override fun read(r: PacketReader): GroupJoinFailedOut = readGroupJoinFailedOut(r)

    override fun write(w: PacketWriter, m: GroupJoinFailedOut) = writeGroupJoinFailedOut(w, m)

    private fun readGroupJoinFailedOut(r: PacketReader): GroupJoinFailedOut {
        val vInt1 = r.int()
        return GroupJoinFailedOut(vInt1)
    }

    private fun writeGroupJoinFailedOut(w: PacketWriter, m: GroupJoinFailedOut) {
        w.int(m.int1)
    }
}
