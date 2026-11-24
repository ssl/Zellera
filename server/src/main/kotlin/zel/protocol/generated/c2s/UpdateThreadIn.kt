package zel.protocol.generated

data class UpdateThreadIn(
    val guildId: Int,
    val threadId: Int,
    val isPinned: Boolean,
    val isLocked: Boolean,
)

object UpdateThreadInCodec : IncomingCodec<UpdateThreadIn> {
    override val name = "UpdateThreadIn"

    override fun read(r: PacketReader): UpdateThreadIn = readUpdateThreadIn(r)

    override fun write(w: PacketWriter, m: UpdateThreadIn) = writeUpdateThreadIn(w, m)

    private fun readUpdateThreadIn(r: PacketReader): UpdateThreadIn {
        val vGuildId = r.int()
        val vThreadId = r.int()
        val vIsPinned = r.boolean()
        val vIsLocked = r.boolean()
        return UpdateThreadIn(vGuildId, vThreadId, vIsPinned, vIsLocked)
    }

    private fun writeUpdateThreadIn(w: PacketWriter, m: UpdateThreadIn) {
        w.int(m.guildId)
        w.int(m.threadId)
        w.boolean(m.isPinned)
        w.boolean(m.isLocked)
    }
}
