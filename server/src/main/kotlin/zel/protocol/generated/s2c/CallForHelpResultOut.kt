package zel.protocol.generated

data class CallForHelpResultOut(
    val int1: Int,
    val string1: String,
)

object CallForHelpResultOutCodec : OutgoingCodec<CallForHelpResultOut> {
    override val name = "CallForHelpResultOut"

    override fun read(r: PacketReader): CallForHelpResultOut = readCallForHelpResultOut(r)

    override fun write(w: PacketWriter, m: CallForHelpResultOut) = writeCallForHelpResultOut(w, m)

    private fun readCallForHelpResultOut(r: PacketReader): CallForHelpResultOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return CallForHelpResultOut(vInt1, vString1)
    }

    private fun writeCallForHelpResultOut(w: PacketWriter, m: CallForHelpResultOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
