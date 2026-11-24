package zel.protocol.generated

data class RoomAdEventTabAdClickedIn(
    val int1: Int,
    val string1: String,
    val int2: Int,
)

object RoomAdEventTabAdClickedInCodec : IncomingCodec<RoomAdEventTabAdClickedIn> {
    override val name = "RoomAdEventTabAdClickedIn"

    override fun read(r: PacketReader): RoomAdEventTabAdClickedIn = readRoomAdEventTabAdClickedIn(r)

    override fun write(w: PacketWriter, m: RoomAdEventTabAdClickedIn) = writeRoomAdEventTabAdClickedIn(w, m)

    private fun readRoomAdEventTabAdClickedIn(r: PacketReader): RoomAdEventTabAdClickedIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return RoomAdEventTabAdClickedIn(vInt1, vString1, vInt2)
    }

    private fun writeRoomAdEventTabAdClickedIn(w: PacketWriter, m: RoomAdEventTabAdClickedIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }
}
