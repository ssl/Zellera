package zel.protocol.generated

data class GroupMembershipRequestedOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val string1: String,
    val string2: String,
    val string3: String,
)

object GroupMembershipRequestedOutCodec : OutgoingCodec<GroupMembershipRequestedOut> {
    override val name = "GroupMembershipRequestedOut"

    override fun read(r: PacketReader): GroupMembershipRequestedOut = readGroupMembershipRequestedOut(r)

    override fun write(w: PacketWriter, m: GroupMembershipRequestedOut) = writeGroupMembershipRequestedOut(w, m)

    private fun readGroupMembershipRequestedOut(r: PacketReader): GroupMembershipRequestedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return GroupMembershipRequestedOut(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun writeGroupMembershipRequestedOut(w: PacketWriter, m: GroupMembershipRequestedOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }
}
