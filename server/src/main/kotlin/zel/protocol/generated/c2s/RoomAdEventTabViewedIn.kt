package zel.protocol.generated

data object RoomAdEventTabViewedIn

object RoomAdEventTabViewedInCodec : IncomingCodec<RoomAdEventTabViewedIn> {
    override val name = "RoomAdEventTabViewedIn"

    override fun read(r: PacketReader): RoomAdEventTabViewedIn = RoomAdEventTabViewedIn

    override fun write(w: PacketWriter, m: RoomAdEventTabViewedIn) = writeRoomAdEventTabViewedIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRoomAdEventTabViewedIn(w: PacketWriter, m: RoomAdEventTabViewedIn) {
    }
}
