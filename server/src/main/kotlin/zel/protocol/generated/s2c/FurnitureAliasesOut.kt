package zel.protocol.generated

data class FurnitureAliasesOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
    )
}

object FurnitureAliasesOutCodec : OutgoingCodec<FurnitureAliasesOut> {
    override val name = "FurnitureAliasesOut"

    override fun read(r: PacketReader): FurnitureAliasesOut = readFurnitureAliasesOut(r)

    override fun write(w: PacketWriter, m: FurnitureAliasesOut) = writeFurnitureAliasesOut(w, m)

    private fun readFurnitureAliasesOutList1Item(r: PacketReader): FurnitureAliasesOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return FurnitureAliasesOut.List1Item(vString1, vString2)
    }

    private fun readFurnitureAliasesOut(r: PacketReader): FurnitureAliasesOut {
        val vList1 = List(r.countInt(4)) { readFurnitureAliasesOutList1Item(r) }
        return FurnitureAliasesOut(vList1)
    }

    private fun writeFurnitureAliasesOutList1Item(w: PacketWriter, m: FurnitureAliasesOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeFurnitureAliasesOut(w: PacketWriter, m: FurnitureAliasesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFurnitureAliasesOutList1Item(w, it) }
    }
}
