package zel.protocol.generated

data class WiredFurniSelectorOut(
    val int1: Int,
    val list1: List<Int>,
    val list2: List<Int>,
    val int2: Int,
    val int3: Int,
    val string1: String,
    val list3: List<Int>,
    val list4: List<String>,
    val list5: List<Int>,
    val list6: List<Int>,
    val int4: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val list7: List<List7Item>,
    val list8: List<List8Item>,
    val list9: List<Int>,
    val list10: List<Int>,
    val boolean4: Boolean,
    val rest: ByteArray,
) {
    data class List7Item(
        val list1: List<Int>,
    )

    data class List8Item(
        val list1: List<Int>,
    )
}

object WiredFurniSelectorOutCodec : OutgoingCodec<WiredFurniSelectorOut> {
    override val name = "WiredFurniSelectorOut"

    override fun read(r: PacketReader): WiredFurniSelectorOut = readWiredFurniSelectorOut(r)

    override fun write(w: PacketWriter, m: WiredFurniSelectorOut) = writeWiredFurniSelectorOut(w, m)

    private fun readWiredFurniSelectorOutList7Item(r: PacketReader): WiredFurniSelectorOut.List7Item {
        val vList1 = List(r.countInt(4)) { r.int() }
        return WiredFurniSelectorOut.List7Item(vList1)
    }

    private fun readWiredFurniSelectorOutList8Item(r: PacketReader): WiredFurniSelectorOut.List8Item {
        val vList1 = List(r.countInt(4)) { r.int() }
        return WiredFurniSelectorOut.List8Item(vList1)
    }

    private fun readWiredFurniSelectorOut(r: PacketReader): WiredFurniSelectorOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(4)) { r.int() }
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vList3 = List(r.countInt(4)) { r.int() }
        val vList4 = List(r.countInt(2)) { r.string() }
        val vList5 = List(r.countInt(4)) { r.int() }
        val vList6 = List(r.countInt(4)) { r.int() }
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vList7 = List(r.countInt(4)) { readWiredFurniSelectorOutList7Item(r) }
        val vList8 = List(r.countInt(4)) { readWiredFurniSelectorOutList8Item(r) }
        val vList9 = List(r.countInt(4)) { r.int() }
        val vList10 = List(r.countInt(4)) { r.int() }
        val vBoolean4 = r.boolean()
        val vRest = r.rest()
        return WiredFurniSelectorOut(vInt1, vList1, vList2, vInt2, vInt3, vString1, vList3, vList4, vList5, vList6, vInt4, vBoolean1, vBoolean2, vBoolean3, vList7, vList8, vList9, vList10, vBoolean4, vRest)
    }

    private fun writeWiredFurniSelectorOutList7Item(w: PacketWriter, m: WiredFurniSelectorOut.List7Item) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }

    private fun writeWiredFurniSelectorOutList8Item(w: PacketWriter, m: WiredFurniSelectorOut.List8Item) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }

    private fun writeWiredFurniSelectorOut(w: PacketWriter, m: WiredFurniSelectorOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.int(it) }
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.countInt(m.list3.size)
        m.list3.forEach { w.int(it) }
        w.countInt(m.list4.size)
        m.list4.forEach { w.string(it) }
        w.countInt(m.list5.size)
        m.list5.forEach { w.int(it) }
        w.countInt(m.list6.size)
        m.list6.forEach { w.int(it) }
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        w.countInt(m.list7.size)
        m.list7.forEach { writeWiredFurniSelectorOutList7Item(w, it) }
        w.countInt(m.list8.size)
        m.list8.forEach { writeWiredFurniSelectorOutList8Item(w, it) }
        w.countInt(m.list9.size)
        m.list9.forEach { w.int(it) }
        w.countInt(m.list10.size)
        m.list10.forEach { w.int(it) }
        w.boolean(m.boolean4)
        w.bytes(m.rest)
    }
}
