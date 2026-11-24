package zel.protocol.generated

data class CfhTopicsInitOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val string1: String,
            val int1: Int,
            val string2: String,
        )
    }
}

object CfhTopicsInitOutCodec : OutgoingCodec<CfhTopicsInitOut> {
    override val name = "CfhTopicsInitOut"

    override fun read(r: PacketReader): CfhTopicsInitOut = readCfhTopicsInitOut(r)

    override fun write(w: PacketWriter, m: CfhTopicsInitOut) = writeCfhTopicsInitOut(w, m)

    private fun readCfhTopicsInitOutList1ItemList1Item(r: PacketReader): CfhTopicsInitOut.List1Item.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        return CfhTopicsInitOut.List1Item.List1Item(vString1, vInt1, vString2)
    }

    private fun readCfhTopicsInitOutList1Item(r: PacketReader): CfhTopicsInitOut.List1Item {
        val vString1 = r.string()
        val vList1 = List(r.countInt(8)) { readCfhTopicsInitOutList1ItemList1Item(r) }
        return CfhTopicsInitOut.List1Item(vString1, vList1)
    }

    private fun readCfhTopicsInitOut(r: PacketReader): CfhTopicsInitOut {
        val vList1 = List(r.countInt(6)) { readCfhTopicsInitOutList1Item(r) }
        return CfhTopicsInitOut(vList1)
    }

    private fun writeCfhTopicsInitOutList1ItemList1Item(w: PacketWriter, m: CfhTopicsInitOut.List1Item.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
    }

    private fun writeCfhTopicsInitOutList1Item(w: PacketWriter, m: CfhTopicsInitOut.List1Item) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeCfhTopicsInitOutList1ItemList1Item(w, it) }
    }

    private fun writeCfhTopicsInitOut(w: PacketWriter, m: CfhTopicsInitOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeCfhTopicsInitOutList1Item(w, it) }
    }
}
