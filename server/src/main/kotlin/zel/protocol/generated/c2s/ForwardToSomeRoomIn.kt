package zel.protocol.generated

data class ForwardToSomeRoomIn(
    val data: String,
)

object ForwardToSomeRoomInCodec : IncomingCodec<ForwardToSomeRoomIn> {
    override val name = "ForwardToSomeRoomIn"

    override fun read(r: PacketReader): ForwardToSomeRoomIn = readForwardToSomeRoomIn(r)

    override fun write(w: PacketWriter, m: ForwardToSomeRoomIn) = writeForwardToSomeRoomIn(w, m)

    private fun readForwardToSomeRoomIn(r: PacketReader): ForwardToSomeRoomIn {
        val vData = r.string()
        return ForwardToSomeRoomIn(vData)
    }

    private fun writeForwardToSomeRoomIn(w: PacketWriter, m: ForwardToSomeRoomIn) {
        w.string(m.data)
    }
}
