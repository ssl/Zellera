package zel.protocol.generated

data object GetPendingCallsForHelpIn

object GetPendingCallsForHelpInCodec : IncomingCodec<GetPendingCallsForHelpIn> {
    override val name = "GetPendingCallsForHelpIn"

    override fun read(r: PacketReader): GetPendingCallsForHelpIn = GetPendingCallsForHelpIn

    override fun write(w: PacketWriter, m: GetPendingCallsForHelpIn) = writeGetPendingCallsForHelpIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPendingCallsForHelpIn(w: PacketWriter, m: GetPendingCallsForHelpIn) {
    }
}
