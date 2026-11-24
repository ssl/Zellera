package zel.protocol.generated

data class ModerateMessageIn(
    val guildId: Int,
    val threadId: Int,
    val messageId: Int,
    val state: Int,
)

object ModerateMessageInCodec : IncomingCodec<ModerateMessageIn> {
    override val name = "ModerateMessageIn"

    override fun read(r: PacketReader): ModerateMessageIn = readModerateMessageIn(r)

    override fun write(w: PacketWriter, m: ModerateMessageIn) = writeModerateMessageIn(w, m)

    private fun readModerateMessageIn(r: PacketReader): ModerateMessageIn {
        val vGuildId = r.int()
        val vThreadId = r.int()
        val vMessageId = r.int()
        val vState = r.int()
        return ModerateMessageIn(vGuildId, vThreadId, vMessageId, vState)
    }

    private fun writeModerateMessageIn(w: PacketWriter, m: ModerateMessageIn) {
        w.int(m.guildId)
        w.int(m.threadId)
        w.int(m.messageId)
        w.int(m.state)
    }
}
