package zel.protocol.generated

data class CraftableProductsOut(
    val list1: List<List1Item>,
    val list2: List<String>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val string3: String,
    )
}

object CraftableProductsOutCodec : OutgoingCodec<CraftableProductsOut> {
    override val name = "CraftableProductsOut"

    override fun read(r: PacketReader): CraftableProductsOut = readCraftableProductsOut(r)

    override fun write(w: PacketWriter, m: CraftableProductsOut) = writeCraftableProductsOut(w, m)

    private fun readCraftableProductsOutList1Item(r: PacketReader): CraftableProductsOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return CraftableProductsOut.List1Item(vString1, vString2, vString3)
    }

    private fun readCraftableProductsOut(r: PacketReader): CraftableProductsOut {
        val vList1 = List(r.countInt(6)) { readCraftableProductsOutList1Item(r) }
        val vList2 = List(r.countInt(2)) { r.string() }
        return CraftableProductsOut(vList1, vList2)
    }

    private fun writeCraftableProductsOutList1Item(w: PacketWriter, m: CraftableProductsOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeCraftableProductsOut(w: PacketWriter, m: CraftableProductsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeCraftableProductsOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.string(it) }
    }
}
