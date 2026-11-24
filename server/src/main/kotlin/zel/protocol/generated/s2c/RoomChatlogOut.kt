package zel.protocol.generated

data class RoomChatlogOut(
    val byte1: Byte,
    val rest: ByteArray,
)

object RoomChatlogOutCodec : OutgoingCodec<RoomChatlogOut> {
    override val name = "RoomChatlogOut"

    override fun read(r: PacketReader): RoomChatlogOut = readRoomChatlogOut(r)

    override fun write(w: PacketWriter, m: RoomChatlogOut) = writeRoomChatlogOut(w, m)

    private fun readRoomChatlogOut(r: PacketReader): RoomChatlogOut {
        val vByte1 = r.byte()
        val vRest = r.rest()
        return RoomChatlogOut(vByte1, vRest)
    }

    private fun writeRoomChatlogOut(w: PacketWriter, m: RoomChatlogOut) {
        w.byte(m.byte1)
        w.bytes(m.rest)
    }
}
