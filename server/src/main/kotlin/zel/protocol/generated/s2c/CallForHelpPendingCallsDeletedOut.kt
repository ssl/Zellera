package zel.protocol.generated

data object CallForHelpPendingCallsDeletedOut

object CallForHelpPendingCallsDeletedOutCodec : OutgoingCodec<CallForHelpPendingCallsDeletedOut> {
    override val name = "CallForHelpPendingCallsDeletedOut"

    override fun read(r: PacketReader): CallForHelpPendingCallsDeletedOut = CallForHelpPendingCallsDeletedOut

    override fun write(w: PacketWriter, m: CallForHelpPendingCallsDeletedOut) = writeCallForHelpPendingCallsDeletedOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCallForHelpPendingCallsDeletedOut(w: PacketWriter, m: CallForHelpPendingCallsDeletedOut) {
    }
}
