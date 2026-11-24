package zel.protocol.generated

data object WiredGetRoomStatsIn

object WiredGetRoomStatsInCodec : IncomingCodec<WiredGetRoomStatsIn> {
    override val name = "WiredGetRoomStatsIn"

    override fun read(r: PacketReader): WiredGetRoomStatsIn = WiredGetRoomStatsIn

    override fun write(w: PacketWriter, m: WiredGetRoomStatsIn) = writeWiredGetRoomStatsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredGetRoomStatsIn(w: PacketWriter, m: WiredGetRoomStatsIn) {
    }
}
