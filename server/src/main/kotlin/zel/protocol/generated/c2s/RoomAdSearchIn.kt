package zel.protocol.generated

data class RoomAdSearchIn(
    val adIndex: Int,
    val searchType: Int,
)

object RoomAdSearchInCodec : IncomingCodec<RoomAdSearchIn> {
    override val name = "RoomAdSearchIn"

    override fun read(r: PacketReader): RoomAdSearchIn = readRoomAdSearchIn(r)

    override fun write(w: PacketWriter, m: RoomAdSearchIn) = writeRoomAdSearchIn(w, m)

    private fun readRoomAdSearchIn(r: PacketReader): RoomAdSearchIn {
        val vAdIndex = r.int()
        val vSearchType = r.int()
        return RoomAdSearchIn(vAdIndex, vSearchType)
    }

    private fun writeRoomAdSearchIn(w: PacketWriter, m: RoomAdSearchIn) {
        w.int(m.adIndex)
        w.int(m.searchType)
    }
}
