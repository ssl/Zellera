package zel.protocol.generated

data class CallForHelpReplyOut(
    val string1: String,
)

object CallForHelpReplyOutCodec : OutgoingCodec<CallForHelpReplyOut> {
    override val name = "CallForHelpReplyOut"

    override fun read(r: PacketReader): CallForHelpReplyOut = readCallForHelpReplyOut(r)

    override fun write(w: PacketWriter, m: CallForHelpReplyOut) = writeCallForHelpReplyOut(w, m)

    private fun readCallForHelpReplyOut(r: PacketReader): CallForHelpReplyOut {
        val vString1 = r.string()
        return CallForHelpReplyOut(vString1)
    }

    private fun writeCallForHelpReplyOut(w: PacketWriter, m: CallForHelpReplyOut) {
        w.string(m.string1)
    }
}
