package zel.protocol.generated

data object DeletePendingCallsForHelpIn

object DeletePendingCallsForHelpInCodec : IncomingCodec<DeletePendingCallsForHelpIn> {
    override val name = "DeletePendingCallsForHelpIn"

    override fun read(r: PacketReader): DeletePendingCallsForHelpIn = DeletePendingCallsForHelpIn

    override fun write(w: PacketWriter, m: DeletePendingCallsForHelpIn) = writeDeletePendingCallsForHelpIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeDeletePendingCallsForHelpIn(w: PacketWriter, m: DeletePendingCallsForHelpIn) {
    }
}
