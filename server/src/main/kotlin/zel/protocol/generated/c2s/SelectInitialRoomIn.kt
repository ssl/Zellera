package zel.protocol.generated

data class SelectInitialRoomIn(
    val string1: String,
)

object SelectInitialRoomInCodec : IncomingCodec<SelectInitialRoomIn> {
    override val name = "SelectInitialRoomIn"

    override fun read(r: PacketReader): SelectInitialRoomIn = readSelectInitialRoomIn(r)

    override fun write(w: PacketWriter, m: SelectInitialRoomIn) = writeSelectInitialRoomIn(w, m)

    private fun readSelectInitialRoomIn(r: PacketReader): SelectInitialRoomIn {
        val vString1 = r.string()
        return SelectInitialRoomIn(vString1)
    }

    private fun writeSelectInitialRoomIn(w: PacketWriter, m: SelectInitialRoomIn) {
        w.string(m.string1)
    }
}
