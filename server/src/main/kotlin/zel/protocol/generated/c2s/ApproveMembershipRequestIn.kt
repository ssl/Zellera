package zel.protocol.generated

data class ApproveMembershipRequestIn(
    val guildId: Int,
    val userId: Int,
)

object ApproveMembershipRequestInCodec : IncomingCodec<ApproveMembershipRequestIn> {
    override val name = "ApproveMembershipRequestIn"

    override fun read(r: PacketReader): ApproveMembershipRequestIn = readApproveMembershipRequestIn(r)

    override fun write(w: PacketWriter, m: ApproveMembershipRequestIn) = writeApproveMembershipRequestIn(w, m)

    private fun readApproveMembershipRequestIn(r: PacketReader): ApproveMembershipRequestIn {
        val vGuildId = r.int()
        val vUserId = r.int()
        return ApproveMembershipRequestIn(vGuildId, vUserId)
    }

    private fun writeApproveMembershipRequestIn(w: PacketWriter, m: ApproveMembershipRequestIn) {
        w.int(m.guildId)
        w.int(m.userId)
    }
}
