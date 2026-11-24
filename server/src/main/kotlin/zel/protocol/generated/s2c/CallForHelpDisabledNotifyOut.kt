package zel.protocol.generated

data class CallForHelpDisabledNotifyOut(
    val string1: String,
)

object CallForHelpDisabledNotifyOutCodec : OutgoingCodec<CallForHelpDisabledNotifyOut> {
    override val name = "CallForHelpDisabledNotifyOut"

    override fun read(r: PacketReader): CallForHelpDisabledNotifyOut = readCallForHelpDisabledNotifyOut(r)

    override fun write(w: PacketWriter, m: CallForHelpDisabledNotifyOut) = writeCallForHelpDisabledNotifyOut(w, m)

    private fun readCallForHelpDisabledNotifyOut(r: PacketReader): CallForHelpDisabledNotifyOut {
        val vString1 = r.string()
        return CallForHelpDisabledNotifyOut(vString1)
    }

    private fun writeCallForHelpDisabledNotifyOut(w: PacketWriter, m: CallForHelpDisabledNotifyOut) {
        w.string(m.string1)
    }
}
