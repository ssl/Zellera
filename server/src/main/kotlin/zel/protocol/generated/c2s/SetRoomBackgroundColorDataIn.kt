package zel.protocol.generated

data class SetRoomBackgroundColorDataIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object SetRoomBackgroundColorDataInCodec : IncomingCodec<SetRoomBackgroundColorDataIn> {
    override val name = "SetRoomBackgroundColorDataIn"

    override fun read(r: PacketReader): SetRoomBackgroundColorDataIn = readSetRoomBackgroundColorDataIn(r)

    override fun write(w: PacketWriter, m: SetRoomBackgroundColorDataIn) = writeSetRoomBackgroundColorDataIn(w, m)

    private fun readSetRoomBackgroundColorDataIn(r: PacketReader): SetRoomBackgroundColorDataIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return SetRoomBackgroundColorDataIn(vInt1, vInt2, vInt3, vInt4)
    }

    private fun writeSetRoomBackgroundColorDataIn(w: PacketWriter, m: SetRoomBackgroundColorDataIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
