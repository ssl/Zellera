package zel.protocol.generated

data class GetOfficialRoomsIn(
    val adIndex: Int,
)

object GetOfficialRoomsInCodec : IncomingCodec<GetOfficialRoomsIn> {
    override val name = "GetOfficialRoomsIn"

    override fun read(r: PacketReader): GetOfficialRoomsIn = readGetOfficialRoomsIn(r)

    override fun write(w: PacketWriter, m: GetOfficialRoomsIn) = writeGetOfficialRoomsIn(w, m)

    private fun readGetOfficialRoomsIn(r: PacketReader): GetOfficialRoomsIn {
        val vAdIndex = r.int()
        return GetOfficialRoomsIn(vAdIndex)
    }

    private fun writeGetOfficialRoomsIn(w: PacketWriter, m: GetOfficialRoomsIn) {
        w.int(m.adIndex)
    }
}
