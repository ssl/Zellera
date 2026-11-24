package zel.protocol.generated

data class FurniRecolorUpdateOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val list1: List<Int>,
        val short1: Short,
        val short2: Short,
        val short3: Short,
    )
}

object FurniRecolorUpdateOutCodec : OutgoingCodec<FurniRecolorUpdateOut> {
    override val name = "FurniRecolorUpdateOut"

    override fun read(r: PacketReader): FurniRecolorUpdateOut = readFurniRecolorUpdateOut(r)

    override fun write(w: PacketWriter, m: FurniRecolorUpdateOut) = writeFurniRecolorUpdateOut(w, m)

    private fun readFurniRecolorUpdateOutList1Item(r: PacketReader): FurniRecolorUpdateOut.List1Item {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vShort1 = r.short()
        val vShort2 = r.short()
        val vShort3 = r.short()
        return FurniRecolorUpdateOut.List1Item(vList1, vShort1, vShort2, vShort3)
    }

    private fun readFurniRecolorUpdateOut(r: PacketReader): FurniRecolorUpdateOut {
        val vList1 = List(r.countInt(10)) { readFurniRecolorUpdateOutList1Item(r) }
        return FurniRecolorUpdateOut(vList1)
    }

    private fun writeFurniRecolorUpdateOutList1Item(w: PacketWriter, m: FurniRecolorUpdateOut.List1Item) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.short(m.short1)
        w.short(m.short2)
        w.short(m.short3)
    }

    private fun writeFurniRecolorUpdateOut(w: PacketWriter, m: FurniRecolorUpdateOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFurniRecolorUpdateOutList1Item(w, it) }
    }
}
