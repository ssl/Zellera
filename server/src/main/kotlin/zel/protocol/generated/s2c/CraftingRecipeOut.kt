package zel.protocol.generated

data class CraftingRecipeOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object CraftingRecipeOutCodec : OutgoingCodec<CraftingRecipeOut> {
    override val name = "CraftingRecipeOut"

    override fun read(r: PacketReader): CraftingRecipeOut = readCraftingRecipeOut(r)

    override fun write(w: PacketWriter, m: CraftingRecipeOut) = writeCraftingRecipeOut(w, m)

    private fun readCraftingRecipeOutList1Item(r: PacketReader): CraftingRecipeOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return CraftingRecipeOut.List1Item(vInt1, vString1)
    }

    private fun readCraftingRecipeOut(r: PacketReader): CraftingRecipeOut {
        val vList1 = List(r.countInt(6)) { readCraftingRecipeOutList1Item(r) }
        return CraftingRecipeOut(vList1)
    }

    private fun writeCraftingRecipeOutList1Item(w: PacketWriter, m: CraftingRecipeOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeCraftingRecipeOut(w: PacketWriter, m: CraftingRecipeOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeCraftingRecipeOutList1Item(w, it) }
    }
}
