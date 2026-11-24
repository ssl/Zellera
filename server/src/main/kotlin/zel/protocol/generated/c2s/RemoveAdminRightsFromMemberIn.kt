package zel.protocol.generated

data class RemoveAdminRightsFromMemberIn(
    val guildId: Int,
    val userId: Int,
)

object RemoveAdminRightsFromMemberInCodec : IncomingCodec<RemoveAdminRightsFromMemberIn> {
    override val name = "RemoveAdminRightsFromMemberIn"

    override fun read(r: PacketReader): RemoveAdminRightsFromMemberIn = readRemoveAdminRightsFromMemberIn(r)

    override fun write(w: PacketWriter, m: RemoveAdminRightsFromMemberIn) = writeRemoveAdminRightsFromMemberIn(w, m)

    private fun readRemoveAdminRightsFromMemberIn(r: PacketReader): RemoveAdminRightsFromMemberIn {
        val vGuildId = r.int()
        val vUserId = r.int()
        return RemoveAdminRightsFromMemberIn(vGuildId, vUserId)
    }

    private fun writeRemoveAdminRightsFromMemberIn(w: PacketWriter, m: RemoveAdminRightsFromMemberIn) {
        w.int(m.guildId)
        w.int(m.userId)
    }
}
