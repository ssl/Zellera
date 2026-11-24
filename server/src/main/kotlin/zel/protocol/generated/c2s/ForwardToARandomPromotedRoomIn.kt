package zel.protocol.generated

data class ForwardToARandomPromotedRoomIn(
    val string1: String,
)

object ForwardToARandomPromotedRoomInCodec : IncomingCodec<ForwardToARandomPromotedRoomIn> {
    override val name = "ForwardToARandomPromotedRoomIn"

    override fun read(r: PacketReader): ForwardToARandomPromotedRoomIn = readForwardToARandomPromotedRoomIn(r)

    override fun write(w: PacketWriter, m: ForwardToARandomPromotedRoomIn) = writeForwardToARandomPromotedRoomIn(w, m)

    private fun readForwardToARandomPromotedRoomIn(r: PacketReader): ForwardToARandomPromotedRoomIn {
        val vString1 = r.string()
        return ForwardToARandomPromotedRoomIn(vString1)
    }

    private fun writeForwardToARandomPromotedRoomIn(w: PacketWriter, m: ForwardToARandomPromotedRoomIn) {
        w.string(m.string1)
    }
}
