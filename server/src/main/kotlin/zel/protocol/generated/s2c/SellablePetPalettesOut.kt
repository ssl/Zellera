package zel.protocol.generated

data class SellablePetPalettesOut(
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val boolean1: Boolean,
        val boolean2: Boolean,
    )
}

object SellablePetPalettesOutCodec : OutgoingCodec<SellablePetPalettesOut> {
    override val name = "SellablePetPalettesOut"

    override fun read(r: PacketReader): SellablePetPalettesOut = readSellablePetPalettesOut(r)

    override fun write(w: PacketWriter, m: SellablePetPalettesOut) = writeSellablePetPalettesOut(w, m)

    private fun readSellablePetPalettesOutList1Item(r: PacketReader): SellablePetPalettesOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return SellablePetPalettesOut.List1Item(vInt1, vInt2, vInt3, vBoolean1, vBoolean2)
    }

    private fun readSellablePetPalettesOut(r: PacketReader): SellablePetPalettesOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(14)) { readSellablePetPalettesOutList1Item(r) }
        return SellablePetPalettesOut(vString1, vList1)
    }

    private fun writeSellablePetPalettesOutList1Item(w: PacketWriter, m: SellablePetPalettesOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }

    private fun writeSellablePetPalettesOut(w: PacketWriter, m: SellablePetPalettesOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeSellablePetPalettesOutList1Item(w, it) }
    }
}
