package zel.protocol.generated

data class GetCraftableProductsIn(
    val itemId: Int,
)

object GetCraftableProductsInCodec : IncomingCodec<GetCraftableProductsIn> {
    override val name = "GetCraftableProductsIn"

    override fun read(r: PacketReader): GetCraftableProductsIn = readGetCraftableProductsIn(r)

    override fun write(w: PacketWriter, m: GetCraftableProductsIn) = writeGetCraftableProductsIn(w, m)

    private fun readGetCraftableProductsIn(r: PacketReader): GetCraftableProductsIn {
        val vItemId = r.int()
        return GetCraftableProductsIn(vItemId)
    }

    private fun writeGetCraftableProductsIn(w: PacketWriter, m: GetCraftableProductsIn) {
        w.int(m.itemId)
    }
}
