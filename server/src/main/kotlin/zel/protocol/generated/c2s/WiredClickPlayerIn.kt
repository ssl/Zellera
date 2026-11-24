package zel.protocol.generated

data class WiredClickPlayerIn(
    val roomIndex: Int,
)

object WiredClickPlayerInCodec : IncomingCodec<WiredClickPlayerIn> {
    override val name = "WiredClickPlayerIn"

    override fun read(r: PacketReader): WiredClickPlayerIn = readWiredClickPlayerIn(r)

    override fun write(w: PacketWriter, m: WiredClickPlayerIn) = writeWiredClickPlayerIn(w, m)

    private fun readWiredClickPlayerIn(r: PacketReader): WiredClickPlayerIn {
        val vRoomIndex = r.int()
        return WiredClickPlayerIn(vRoomIndex)
    }

    private fun writeWiredClickPlayerIn(w: PacketWriter, m: WiredClickPlayerIn) {
        w.int(m.roomIndex)
    }
}
