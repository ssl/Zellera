package zel.protocol.generated

data class ClickFurniIn(
    val itemId: Int,
    val int1: Int,
)

object ClickFurniInCodec : IncomingCodec<ClickFurniIn> {
    override val name = "ClickFurniIn"

    override fun read(r: PacketReader): ClickFurniIn = readClickFurniIn(r)

    override fun write(w: PacketWriter, m: ClickFurniIn) = writeClickFurniIn(w, m)

    private fun readClickFurniIn(r: PacketReader): ClickFurniIn {
        val vItemId = r.int()
        val vInt1 = r.int()
        return ClickFurniIn(vItemId, vInt1)
    }

    private fun writeClickFurniIn(w: PacketWriter, m: ClickFurniIn) {
        w.int(m.itemId)
        w.int(m.int1)
    }
}
