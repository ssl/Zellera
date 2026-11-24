package zel.protocol.generated

data object WiredClearErrorLogsIn

object WiredClearErrorLogsInCodec : IncomingCodec<WiredClearErrorLogsIn> {
    override val name = "WiredClearErrorLogsIn"

    override fun read(r: PacketReader): WiredClearErrorLogsIn = WiredClearErrorLogsIn

    override fun write(w: PacketWriter, m: WiredClearErrorLogsIn) = writeWiredClearErrorLogsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredClearErrorLogsIn(w: PacketWriter, m: WiredClearErrorLogsIn) {
    }
}
