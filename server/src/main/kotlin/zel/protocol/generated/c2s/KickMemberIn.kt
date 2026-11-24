package zel.protocol.generated

data class KickMemberIn(
    val guildId: Int,
    val userId: Int,
    val boolean1: Boolean,
)

object KickMemberInCodec : IncomingCodec<KickMemberIn> {
    override val name = "KickMemberIn"

    override fun read(r: PacketReader): KickMemberIn = readKickMemberIn(r)

    override fun write(w: PacketWriter, m: KickMemberIn) = writeKickMemberIn(w, m)

    private fun readKickMemberIn(r: PacketReader): KickMemberIn {
        val vGuildId = r.int()
        val vUserId = r.int()
        val vBoolean1 = r.boolean()
        return KickMemberIn(vGuildId, vUserId, vBoolean1)
    }

    private fun writeKickMemberIn(w: PacketWriter, m: KickMemberIn) {
        w.int(m.guildId)
        w.int(m.userId)
        w.boolean(m.boolean1)
    }
}
