package zel.protocol.generated

data class ApproveAllMembershipRequestsIn(
    val guildId: Int,
)

object ApproveAllMembershipRequestsInCodec : IncomingCodec<ApproveAllMembershipRequestsIn> {
    override val name = "ApproveAllMembershipRequestsIn"

    override fun read(r: PacketReader): ApproveAllMembershipRequestsIn = readApproveAllMembershipRequestsIn(r)

    override fun write(w: PacketWriter, m: ApproveAllMembershipRequestsIn) = writeApproveAllMembershipRequestsIn(w, m)

    private fun readApproveAllMembershipRequestsIn(r: PacketReader): ApproveAllMembershipRequestsIn {
        val vGuildId = r.int()
        return ApproveAllMembershipRequestsIn(vGuildId)
    }

    private fun writeApproveAllMembershipRequestsIn(w: PacketWriter, m: ApproveAllMembershipRequestsIn) {
        w.int(m.guildId)
    }
}
