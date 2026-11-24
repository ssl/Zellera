package zel.protocol.generated

data class GuestRoomSearchResultOut(
    val int1: Int,
    val string1: String,
    val rest: ByteArray,
)

object GuestRoomSearchResultOutCodec : OutgoingCodec<GuestRoomSearchResultOut> {
    override val name = "GuestRoomSearchResultOut"

    override fun read(r: PacketReader): GuestRoomSearchResultOut = readGuestRoomSearchResultOut(r)

    override fun write(w: PacketWriter, m: GuestRoomSearchResultOut) = writeGuestRoomSearchResultOut(w, m)

    private fun readGuestRoomSearchResultOut(r: PacketReader): GuestRoomSearchResultOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vRest = r.rest()
        return GuestRoomSearchResultOut(vInt1, vString1, vRest)
    }

    private fun writeGuestRoomSearchResultOut(w: PacketWriter, m: GuestRoomSearchResultOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.bytes(m.rest)
    }
}
