package zel.protocol.generated

data class RequestRoomPropertySetIn(
    val itemId: Int,
)

object RequestRoomPropertySetInCodec : IncomingCodec<RequestRoomPropertySetIn> {
    override val name = "RequestRoomPropertySetIn"

    override fun read(r: PacketReader): RequestRoomPropertySetIn = readRequestRoomPropertySetIn(r)

    override fun write(w: PacketWriter, m: RequestRoomPropertySetIn) = writeRequestRoomPropertySetIn(w, m)

    private fun readRequestRoomPropertySetIn(r: PacketReader): RequestRoomPropertySetIn {
        val vItemId = r.int()
        return RequestRoomPropertySetIn(vItemId)
    }

    private fun writeRequestRoomPropertySetIn(w: PacketWriter, m: RequestRoomPropertySetIn) {
        w.int(m.itemId)
    }
}
