package zel.protocol.generated

data class GetFlatControllersIn(
    val requestedRoomId: Int,
)

object GetFlatControllersInCodec : IncomingCodec<GetFlatControllersIn> {
    override val name = "GetFlatControllersIn"

    override fun read(r: PacketReader): GetFlatControllersIn = readGetFlatControllersIn(r)

    override fun write(w: PacketWriter, m: GetFlatControllersIn) = writeGetFlatControllersIn(w, m)

    private fun readGetFlatControllersIn(r: PacketReader): GetFlatControllersIn {
        val vRequestedRoomId = r.int()
        return GetFlatControllersIn(vRequestedRoomId)
    }

    private fun writeGetFlatControllersIn(w: PacketWriter, m: GetFlatControllersIn) {
        w.int(m.requestedRoomId)
    }
}
