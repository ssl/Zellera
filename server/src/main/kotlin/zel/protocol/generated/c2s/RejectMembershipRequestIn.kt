package zel.protocol.generated

data class RejectMembershipRequestIn(
    val guildId: Int,
    val userId: Int,
)

object RejectMembershipRequestInCodec : IncomingCodec<RejectMembershipRequestIn> {
    override val name = "RejectMembershipRequestIn"

    override fun read(r: PacketReader): RejectMembershipRequestIn = readRejectMembershipRequestIn(r)

    override fun write(w: PacketWriter, m: RejectMembershipRequestIn) = writeRejectMembershipRequestIn(w, m)

    private fun readRejectMembershipRequestIn(r: PacketReader): RejectMembershipRequestIn {
        val vGuildId = r.int()
        val vUserId = r.int()
        return RejectMembershipRequestIn(vGuildId, vUserId)
    }

    private fun writeRejectMembershipRequestIn(w: PacketWriter, m: RejectMembershipRequestIn) {
        w.int(m.guildId)
        w.int(m.userId)
    }
}
