package zel.protocol.generated

data object MyRoomRightsSearchIn

object MyRoomRightsSearchInCodec : IncomingCodec<MyRoomRightsSearchIn> {
    override val name = "MyRoomRightsSearchIn"

    override fun read(r: PacketReader): MyRoomRightsSearchIn = MyRoomRightsSearchIn

    override fun write(w: PacketWriter, m: MyRoomRightsSearchIn) = writeMyRoomRightsSearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyRoomRightsSearchIn(w: PacketWriter, m: MyRoomRightsSearchIn) {
    }
}
