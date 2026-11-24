package zel.protocol.generated

data object MyRecommendedRoomsIn

object MyRecommendedRoomsInCodec : IncomingCodec<MyRecommendedRoomsIn> {
    override val name = "MyRecommendedRoomsIn"

    override fun read(r: PacketReader): MyRecommendedRoomsIn = MyRecommendedRoomsIn

    override fun write(w: PacketWriter, m: MyRecommendedRoomsIn) = writeMyRecommendedRoomsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyRecommendedRoomsIn(w: PacketWriter, m: MyRecommendedRoomsIn) {
    }
}
