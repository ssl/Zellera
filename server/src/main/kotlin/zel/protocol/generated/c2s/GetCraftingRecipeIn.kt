package zel.protocol.generated

data class GetCraftingRecipeIn(
    val recipeName: String,
)

object GetCraftingRecipeInCodec : IncomingCodec<GetCraftingRecipeIn> {
    override val name = "GetCraftingRecipeIn"

    override fun read(r: PacketReader): GetCraftingRecipeIn = readGetCraftingRecipeIn(r)

    override fun write(w: PacketWriter, m: GetCraftingRecipeIn) = writeGetCraftingRecipeIn(w, m)

    private fun readGetCraftingRecipeIn(r: PacketReader): GetCraftingRecipeIn {
        val vRecipeName = r.string()
        return GetCraftingRecipeIn(vRecipeName)
    }

    private fun writeGetCraftingRecipeIn(w: PacketWriter, m: GetCraftingRecipeIn) {
        w.string(m.recipeName)
    }
}
