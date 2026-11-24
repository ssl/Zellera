package zel.protocol.generated

data class RoomTextSearchIn(
    val text: String,
)

object RoomTextSearchInCodec : IncomingCodec<RoomTextSearchIn> {
    override val name = "RoomTextSearchIn"

    override fun read(r: PacketReader): RoomTextSearchIn = readRoomTextSearchIn(r)

    override fun write(w: PacketWriter, m: RoomTextSearchIn) = writeRoomTextSearchIn(w, m)

    private fun readRoomTextSearchIn(r: PacketReader): RoomTextSearchIn {
        val vText = r.string()
        return RoomTextSearchIn(vText)
    }

    private fun writeRoomTextSearchIn(w: PacketWriter, m: RoomTextSearchIn) {
        w.string(m.text)
    }
}
