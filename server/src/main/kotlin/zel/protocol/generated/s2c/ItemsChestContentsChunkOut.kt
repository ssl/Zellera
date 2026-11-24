package zel.protocol.generated

data class ItemsChestContentsChunkOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val rest: ByteArray,
)

object ItemsChestContentsChunkOutCodec : OutgoingCodec<ItemsChestContentsChunkOut> {
    override val name = "ItemsChestContentsChunkOut"

    override fun read(r: PacketReader): ItemsChestContentsChunkOut = readItemsChestContentsChunkOut(r)

    override fun write(w: PacketWriter, m: ItemsChestContentsChunkOut) = writeItemsChestContentsChunkOut(w, m)

    private fun readItemsChestContentsChunkOut(r: PacketReader): ItemsChestContentsChunkOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vRest = r.rest()
        return ItemsChestContentsChunkOut(vInt1, vInt2, vInt3, vRest)
    }

    private fun writeItemsChestContentsChunkOut(w: PacketWriter, m: ItemsChestContentsChunkOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.bytes(m.rest)
    }
}
