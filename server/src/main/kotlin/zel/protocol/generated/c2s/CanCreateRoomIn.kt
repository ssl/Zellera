package zel.protocol.generated

data object CanCreateRoomIn

object CanCreateRoomInCodec : IncomingCodec<CanCreateRoomIn> {
    override val name = "CanCreateRoomIn"

    override fun read(r: PacketReader): CanCreateRoomIn = CanCreateRoomIn

    override fun write(w: PacketWriter, m: CanCreateRoomIn) = writeCanCreateRoomIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCanCreateRoomIn(w: PacketWriter, m: CanCreateRoomIn) {
    }
}
