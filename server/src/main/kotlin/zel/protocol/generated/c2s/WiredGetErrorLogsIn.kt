package zel.protocol.generated

data object WiredGetErrorLogsIn

object WiredGetErrorLogsInCodec : IncomingCodec<WiredGetErrorLogsIn> {
    override val name = "WiredGetErrorLogsIn"

    override fun read(r: PacketReader): WiredGetErrorLogsIn = WiredGetErrorLogsIn

    override fun write(w: PacketWriter, m: WiredGetErrorLogsIn) = writeWiredGetErrorLogsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredGetErrorLogsIn(w: PacketWriter, m: WiredGetErrorLogsIn) {
    }
}
