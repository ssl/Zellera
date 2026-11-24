package zel.protocol.generated

data class CustomizePlayerWithFurniIn(
    val itemId: Int,
)

object CustomizePlayerWithFurniInCodec : IncomingCodec<CustomizePlayerWithFurniIn> {
    override val name = "CustomizePlayerWithFurniIn"

    override fun read(r: PacketReader): CustomizePlayerWithFurniIn = readCustomizePlayerWithFurniIn(r)

    override fun write(w: PacketWriter, m: CustomizePlayerWithFurniIn) = writeCustomizePlayerWithFurniIn(w, m)

    private fun readCustomizePlayerWithFurniIn(r: PacketReader): CustomizePlayerWithFurniIn {
        val vItemId = r.int()
        return CustomizePlayerWithFurniIn(vItemId)
    }

    private fun writeCustomizePlayerWithFurniIn(w: PacketWriter, m: CustomizePlayerWithFurniIn) {
        w.int(m.itemId)
    }
}
