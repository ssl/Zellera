package zel.protocol.generated

data class CraftIn(
    val craftingTable: Int,
    val recipe: String,
)

object CraftInCodec : IncomingCodec<CraftIn> {
    override val name = "CraftIn"

    override fun read(r: PacketReader): CraftIn = readCraftIn(r)

    override fun write(w: PacketWriter, m: CraftIn) = writeCraftIn(w, m)

    private fun readCraftIn(r: PacketReader): CraftIn {
        val vCraftingTable = r.int()
        val vRecipe = r.string()
        return CraftIn(vCraftingTable, vRecipe)
    }

    private fun writeCraftIn(w: PacketWriter, m: CraftIn) {
        w.int(m.craftingTable)
        w.string(m.recipe)
    }
}
