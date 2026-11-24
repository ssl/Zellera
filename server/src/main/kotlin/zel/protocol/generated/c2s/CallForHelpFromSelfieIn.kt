package zel.protocol.generated

data class CallForHelpFromSelfieIn(
    val string1: String,
    val int1: Int,
    val int2: Int,
    val string2: String,
    val int3: Int,
)

object CallForHelpFromSelfieInCodec : IncomingCodec<CallForHelpFromSelfieIn> {
    override val name = "CallForHelpFromSelfieIn"

    override fun read(r: PacketReader): CallForHelpFromSelfieIn = readCallForHelpFromSelfieIn(r)

    override fun write(w: PacketWriter, m: CallForHelpFromSelfieIn) = writeCallForHelpFromSelfieIn(w, m)

    private fun readCallForHelpFromSelfieIn(r: PacketReader): CallForHelpFromSelfieIn {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString2 = r.string()
        val vInt3 = r.int()
        return CallForHelpFromSelfieIn(vString1, vInt1, vInt2, vString2, vInt3)
    }

    private fun writeCallForHelpFromSelfieIn(w: PacketWriter, m: CallForHelpFromSelfieIn) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string2)
        w.int(m.int3)
    }
}
