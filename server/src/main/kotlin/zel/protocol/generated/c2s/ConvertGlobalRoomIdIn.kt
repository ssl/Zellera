package zel.protocol.generated

data class ConvertGlobalRoomIdIn(
    val target: String,
)

object ConvertGlobalRoomIdInCodec : IncomingCodec<ConvertGlobalRoomIdIn> {
    override val name = "ConvertGlobalRoomIdIn"

    override fun read(r: PacketReader): ConvertGlobalRoomIdIn = readConvertGlobalRoomIdIn(r)

    override fun write(w: PacketWriter, m: ConvertGlobalRoomIdIn) = writeConvertGlobalRoomIdIn(w, m)

    private fun readConvertGlobalRoomIdIn(r: PacketReader): ConvertGlobalRoomIdIn {
        val vTarget = r.string()
        return ConvertGlobalRoomIdIn(vTarget)
    }

    private fun writeConvertGlobalRoomIdIn(w: PacketWriter, m: ConvertGlobalRoomIdIn) {
        w.string(m.target)
    }
}
