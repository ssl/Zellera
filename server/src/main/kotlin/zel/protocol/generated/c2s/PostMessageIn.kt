package zel.protocol.generated

data class PostMessageIn(
    val guildId: Int,
    val threadId: Int,
    val subject: String,
    val message: String,
)

object PostMessageInCodec : IncomingCodec<PostMessageIn> {
    override val name = "PostMessageIn"

    override fun read(r: PacketReader): PostMessageIn = readPostMessageIn(r)

    override fun write(w: PacketWriter, m: PostMessageIn) = writePostMessageIn(w, m)

    private fun readPostMessageIn(r: PacketReader): PostMessageIn {
        val vGuildId = r.int()
        val vThreadId = r.int()
        val vSubject = r.string()
        val vMessage = r.string()
        return PostMessageIn(vGuildId, vThreadId, vSubject, vMessage)
    }

    private fun writePostMessageIn(w: PacketWriter, m: PostMessageIn) {
        w.int(m.guildId)
        w.int(m.threadId)
        w.string(m.subject)
        w.string(m.message)
    }
}
