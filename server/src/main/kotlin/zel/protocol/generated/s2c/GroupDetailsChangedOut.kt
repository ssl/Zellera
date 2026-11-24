package zel.protocol.generated

data class GroupDetailsChangedOut(
    val int1: Int,
)

object GroupDetailsChangedOutCodec : OutgoingCodec<GroupDetailsChangedOut> {
    override val name = "GroupDetailsChangedOut"

    override fun read(r: PacketReader): GroupDetailsChangedOut = readGroupDetailsChangedOut(r)

    override fun write(w: PacketWriter, m: GroupDetailsChangedOut) = writeGroupDetailsChangedOut(w, m)

    private fun readGroupDetailsChangedOut(r: PacketReader): GroupDetailsChangedOut {
        val vInt1 = r.int()
        return GroupDetailsChangedOut(vInt1)
    }

    private fun writeGroupDetailsChangedOut(w: PacketWriter, m: GroupDetailsChangedOut) {
        w.int(m.int1)
    }
}
