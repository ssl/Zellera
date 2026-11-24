package zel.protocol.generated

data class ConvertedRoomIdOut(
    val string1: String,
    val int1: Int,
)

object ConvertedRoomIdOutCodec : OutgoingCodec<ConvertedRoomIdOut> {
    override val name = "ConvertedRoomIdOut"

    override fun read(r: PacketReader): ConvertedRoomIdOut = readConvertedRoomIdOut(r)

    override fun write(w: PacketWriter, m: ConvertedRoomIdOut) = writeConvertedRoomIdOut(w, m)

    private fun readConvertedRoomIdOut(r: PacketReader): ConvertedRoomIdOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        return ConvertedRoomIdOut(vString1, vInt1)
    }

    private fun writeConvertedRoomIdOut(w: PacketWriter, m: ConvertedRoomIdOut) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
