package zel.protocol.generated

data object GetPopularRoomTagsIn

object GetPopularRoomTagsInCodec : IncomingCodec<GetPopularRoomTagsIn> {
    override val name = "GetPopularRoomTagsIn"

    override fun read(r: PacketReader): GetPopularRoomTagsIn = GetPopularRoomTagsIn

    override fun write(w: PacketWriter, m: GetPopularRoomTagsIn) = writeGetPopularRoomTagsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPopularRoomTagsIn(w: PacketWriter, m: GetPopularRoomTagsIn) {
    }
}
