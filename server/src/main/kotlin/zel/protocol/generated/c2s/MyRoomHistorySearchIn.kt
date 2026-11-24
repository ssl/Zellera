package zel.protocol.generated

data object MyRoomHistorySearchIn

object MyRoomHistorySearchInCodec : IncomingCodec<MyRoomHistorySearchIn> {
    override val name = "MyRoomHistorySearchIn"

    override fun read(r: PacketReader): MyRoomHistorySearchIn = MyRoomHistorySearchIn

    override fun write(w: PacketWriter, m: MyRoomHistorySearchIn) = writeMyRoomHistorySearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyRoomHistorySearchIn(w: PacketWriter, m: MyRoomHistorySearchIn) {
    }
}
