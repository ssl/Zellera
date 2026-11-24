package zel.protocol.generated

data class SelectInitialRoomOut(
    val short1: Short,
    val int1: Int,
)

object SelectInitialRoomOutCodec : OutgoingCodec<SelectInitialRoomOut> {
    override val name = "SelectInitialRoomOut"

    override fun read(r: PacketReader): SelectInitialRoomOut = readSelectInitialRoomOut(r)

    override fun write(w: PacketWriter, m: SelectInitialRoomOut) = writeSelectInitialRoomOut(w, m)

    private fun readSelectInitialRoomOut(r: PacketReader): SelectInitialRoomOut {
        val vShort1 = r.short()
        val vInt1 = r.int()
        return SelectInitialRoomOut(vShort1, vInt1)
    }

    private fun writeSelectInitialRoomOut(w: PacketWriter, m: SelectInitialRoomOut) {
        w.short(m.short1)
        w.int(m.int1)
    }
}
