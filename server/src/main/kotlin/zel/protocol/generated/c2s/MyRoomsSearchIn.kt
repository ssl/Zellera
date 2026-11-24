package zel.protocol.generated

data object MyRoomsSearchIn

object MyRoomsSearchInCodec : IncomingCodec<MyRoomsSearchIn> {
    override val name = "MyRoomsSearchIn"

    override fun read(r: PacketReader): MyRoomsSearchIn = MyRoomsSearchIn

    override fun write(w: PacketWriter, m: MyRoomsSearchIn) = writeMyRoomsSearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyRoomsSearchIn(w: PacketWriter, m: MyRoomsSearchIn) {
    }
}
