package zel.protocol.generated

data object MuteAllInRoomIn

object MuteAllInRoomInCodec : IncomingCodec<MuteAllInRoomIn> {
    override val name = "MuteAllInRoomIn"

    override fun read(r: PacketReader): MuteAllInRoomIn = MuteAllInRoomIn

    override fun write(w: PacketWriter, m: MuteAllInRoomIn) = writeMuteAllInRoomIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMuteAllInRoomIn(w: PacketWriter, m: MuteAllInRoomIn) {
    }
}
