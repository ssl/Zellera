package zel.protocol.generated

data class OfficialRoomsOut(
    val rest: ByteArray,
)

object OfficialRoomsOutCodec : OutgoingCodec<OfficialRoomsOut> {
    override val name = "OfficialRoomsOut"

    override fun read(r: PacketReader): OfficialRoomsOut = readOfficialRoomsOut(r)

    override fun write(w: PacketWriter, m: OfficialRoomsOut) = writeOfficialRoomsOut(w, m)

    private fun readOfficialRoomsOut(r: PacketReader): OfficialRoomsOut {
        val vRest = r.rest()
        return OfficialRoomsOut(vRest)
    }

    private fun writeOfficialRoomsOut(w: PacketWriter, m: OfficialRoomsOut) {
        w.bytes(m.rest)
    }
}
