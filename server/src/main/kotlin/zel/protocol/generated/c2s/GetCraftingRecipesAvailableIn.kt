package zel.protocol.generated

data class GetCraftingRecipesAvailableIn(
    val altarId: Int,
    val itemIds: List<Int>,
)

object GetCraftingRecipesAvailableInCodec : IncomingCodec<GetCraftingRecipesAvailableIn> {
    override val name = "GetCraftingRecipesAvailableIn"

    override fun read(r: PacketReader): GetCraftingRecipesAvailableIn = readGetCraftingRecipesAvailableIn(r)

    override fun write(w: PacketWriter, m: GetCraftingRecipesAvailableIn) = writeGetCraftingRecipesAvailableIn(w, m)

    private fun readGetCraftingRecipesAvailableIn(r: PacketReader): GetCraftingRecipesAvailableIn {
        val vAltarId = r.int()
        val vItemIds = List(r.countInt(4)) { r.int() }
        return GetCraftingRecipesAvailableIn(vAltarId, vItemIds)
    }

    private fun writeGetCraftingRecipesAvailableIn(w: PacketWriter, m: GetCraftingRecipesAvailableIn) {
        w.int(m.altarId)
        w.countInt(m.itemIds.size)
        m.itemIds.forEach { w.int(it) }
    }
}
