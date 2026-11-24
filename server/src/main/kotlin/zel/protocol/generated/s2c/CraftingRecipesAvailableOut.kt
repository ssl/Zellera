package zel.protocol.generated

data class CraftingRecipesAvailableOut(
    val int1: Int,
    val boolean1: Boolean,
)

object CraftingRecipesAvailableOutCodec : OutgoingCodec<CraftingRecipesAvailableOut> {
    override val name = "CraftingRecipesAvailableOut"

    override fun read(r: PacketReader): CraftingRecipesAvailableOut = readCraftingRecipesAvailableOut(r)

    override fun write(w: PacketWriter, m: CraftingRecipesAvailableOut) = writeCraftingRecipesAvailableOut(w, m)

    private fun readCraftingRecipesAvailableOut(r: PacketReader): CraftingRecipesAvailableOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return CraftingRecipesAvailableOut(vInt1, vBoolean1)
    }

    private fun writeCraftingRecipesAvailableOut(w: PacketWriter, m: CraftingRecipesAvailableOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
