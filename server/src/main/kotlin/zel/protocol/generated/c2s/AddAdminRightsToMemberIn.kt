package zel.protocol.generated

data class AddAdminRightsToMemberIn(
    val guildId: Int,
    val userId: Int,
)

object AddAdminRightsToMemberInCodec : IncomingCodec<AddAdminRightsToMemberIn> {
    override val name = "AddAdminRightsToMemberIn"

    override fun read(r: PacketReader): AddAdminRightsToMemberIn = readAddAdminRightsToMemberIn(r)

    override fun write(w: PacketWriter, m: AddAdminRightsToMemberIn) = writeAddAdminRightsToMemberIn(w, m)

    private fun readAddAdminRightsToMemberIn(r: PacketReader): AddAdminRightsToMemberIn {
        val vGuildId = r.int()
        val vUserId = r.int()
        return AddAdminRightsToMemberIn(vGuildId, vUserId)
    }

    private fun writeAddAdminRightsToMemberIn(w: PacketWriter, m: AddAdminRightsToMemberIn) {
        w.int(m.guildId)
        w.int(m.userId)
    }
}
