package zel.protocol.generated

data class UnblockGroupMemberIn(
    val guildId: Int,
    val userId: Int,
)

object UnblockGroupMemberInCodec : IncomingCodec<UnblockGroupMemberIn> {
    override val name = "UnblockGroupMemberIn"

    override fun read(r: PacketReader): UnblockGroupMemberIn = readUnblockGroupMemberIn(r)

    override fun write(w: PacketWriter, m: UnblockGroupMemberIn) = writeUnblockGroupMemberIn(w, m)

    private fun readUnblockGroupMemberIn(r: PacketReader): UnblockGroupMemberIn {
        val vGuildId = r.int()
        val vUserId = r.int()
        return UnblockGroupMemberIn(vGuildId, vUserId)
    }

    private fun writeUnblockGroupMemberIn(w: PacketWriter, m: UnblockGroupMemberIn) {
        w.int(m.guildId)
        w.int(m.userId)
    }
}
