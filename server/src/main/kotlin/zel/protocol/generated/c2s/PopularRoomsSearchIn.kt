package zel.protocol.generated

data class PopularRoomsSearchIn(
    val query: String,
    val adIndex: Int,
)

object PopularRoomsSearchInCodec : IncomingCodec<PopularRoomsSearchIn> {
    override val name = "PopularRoomsSearchIn"

    override fun read(r: PacketReader): PopularRoomsSearchIn = readPopularRoomsSearchIn(r)

    override fun write(w: PacketWriter, m: PopularRoomsSearchIn) = writePopularRoomsSearchIn(w, m)

    private fun readPopularRoomsSearchIn(r: PacketReader): PopularRoomsSearchIn {
        val vQuery = r.string()
        val vAdIndex = r.int()
        return PopularRoomsSearchIn(vQuery, vAdIndex)
    }

    private fun writePopularRoomsSearchIn(w: PacketWriter, m: PopularRoomsSearchIn) {
        w.string(m.query)
        w.int(m.adIndex)
    }
}
