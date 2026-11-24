package zel.protocol.generated

data object RoomEventCancelOut

object RoomEventCancelOutCodec : OutgoingCodec<RoomEventCancelOut> {
    override val name = "RoomEventCancelOut"

    override fun read(r: PacketReader): RoomEventCancelOut = RoomEventCancelOut

    override fun write(w: PacketWriter, m: RoomEventCancelOut) = writeRoomEventCancelOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRoomEventCancelOut(w: PacketWriter, m: RoomEventCancelOut) {
    }
}
