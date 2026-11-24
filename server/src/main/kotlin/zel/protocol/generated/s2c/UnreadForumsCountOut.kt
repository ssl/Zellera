package zel.protocol.generated

data class UnreadForumsCountOut(
    val int1: Int,
)

object UnreadForumsCountOutCodec : OutgoingCodec<UnreadForumsCountOut> {
    override val name = "UnreadForumsCountOut"

    override fun read(r: PacketReader): UnreadForumsCountOut = readUnreadForumsCountOut(r)

    override fun write(w: PacketWriter, m: UnreadForumsCountOut) = writeUnreadForumsCountOut(w, m)

    private fun readUnreadForumsCountOut(r: PacketReader): UnreadForumsCountOut {
        val vInt1 = r.int()
        return UnreadForumsCountOut(vInt1)
    }

    private fun writeUnreadForumsCountOut(w: PacketWriter, m: UnreadForumsCountOut) {
        w.int(m.int1)
    }
}
