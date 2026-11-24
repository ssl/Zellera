package zel.protocol.generated

data class IconShopDataOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val boolean1: Boolean,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val int1: Int,
            val string1: String,
            val int2: Int,
            val int3: Int,
            val int4: Int,
            val int5: Int,
            val int6: Int,
        )
    }
}

object IconShopDataOutCodec : OutgoingCodec<IconShopDataOut> {
    override val name = "IconShopDataOut"

    override fun read(r: PacketReader): IconShopDataOut = readIconShopDataOut(r)

    override fun write(w: PacketWriter, m: IconShopDataOut) = writeIconShopDataOut(w, m)

    private fun readIconShopDataOutList1ItemList1Item(r: PacketReader): IconShopDataOut.List1Item.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        return IconShopDataOut.List1Item.List1Item(vInt1, vString1, vInt2, vInt3, vInt4, vInt5, vInt6)
    }

    private fun readIconShopDataOutList1Item(r: PacketReader): IconShopDataOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vList1 = List(r.countInt(26)) { readIconShopDataOutList1ItemList1Item(r) }
        return IconShopDataOut.List1Item(vInt1, vString1, vBoolean1, vInt2, vInt3, vInt4, vInt5, vInt6, vList1)
    }

    private fun readIconShopDataOut(r: PacketReader): IconShopDataOut {
        val vList1 = List(r.countInt(31)) { readIconShopDataOutList1Item(r) }
        return IconShopDataOut(vList1)
    }

    private fun writeIconShopDataOutList1ItemList1Item(w: PacketWriter, m: IconShopDataOut.List1Item.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
    }

    private fun writeIconShopDataOutList1Item(w: PacketWriter, m: IconShopDataOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.countInt(m.list1.size)
        m.list1.forEach { writeIconShopDataOutList1ItemList1Item(w, it) }
    }

    private fun writeIconShopDataOut(w: PacketWriter, m: IconShopDataOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeIconShopDataOutList1Item(w, it) }
    }
}
