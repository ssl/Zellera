package zel.protocol.generated

data class WiredFurniActionOut(
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
    val int5: Int,
    val boolean1: Boolean,
    val list7: List<List7Item>,
    val list8: List<List8Item>,
    val list9: List<Int>,
    val list10: List<Int>,
    val boolean2: Boolean,
    val rest: ByteArray,
) {
    data class List7Item(
        val list1: List<Int>,
    )

    data class List8Item(
        val list1: List<Int>,
    )
}

object WiredFurniActionOutCodec : OutgoingCodec<WiredFurniActionOut> {
    override val name = "WiredFurniActionOut"

    override fun read(r: PacketReader): WiredFurniActionOut = readWiredFurniActionOut(r)

    override fun write(w: PacketWriter, m: WiredFurniActionOut) = writeWiredFurniActionOut(w, m)

    private fun readWiredFurniActionOutList7Item(r: PacketReader): WiredFurniActionOut.List7Item {
        val vList1 = List(r.countInt(4)) { r.int() }
        return WiredFurniActionOut.List7Item(vList1)
    }

    private fun readWiredFurniActionOutList8Item(r: PacketReader): WiredFurniActionOut.List8Item {
        val vList1 = List(r.countInt(4)) { r.int() }
        return WiredFurniActionOut.List8Item(vList1)
    }

    private fun readWiredFurniActionOut(r: PacketReader): WiredFurniActionOut {
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
        val vInt5 = r.int()
        val vBoolean1 = r.boolean()
        val vList7 = List(r.countInt(4)) { readWiredFurniActionOutList7Item(r) }
        val vList8 = List(r.countInt(4)) { readWiredFurniActionOutList8Item(r) }
        val vList9 = List(r.countInt(4)) { r.int() }
        val vList10 = List(r.countInt(4)) { r.int() }
        val vBoolean2 = r.boolean()
        val vRest = r.rest()
        return WiredFurniActionOut(vInt1, vList1, vList2, vInt2, vInt3, vString1, vList3, vList4, vList5, vList6, vInt4, vInt5, vBoolean1, vList7, vList8, vList9, vList10, vBoolean2, vRest)
    }

    private fun writeWiredFurniActionOutList7Item(w: PacketWriter, m: WiredFurniActionOut.List7Item) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }

    private fun writeWiredFurniActionOutList8Item(w: PacketWriter, m: WiredFurniActionOut.List8Item) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }

    private fun writeWiredFurniActionOut(w: PacketWriter, m: WiredFurniActionOut) {
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
        w.int(m.int5)
        w.boolean(m.boolean1)
        w.countInt(m.list7.size)
        m.list7.forEach { writeWiredFurniActionOutList7Item(w, it) }
        w.countInt(m.list8.size)
        m.list8.forEach { writeWiredFurniActionOutList8Item(w, it) }
        w.countInt(m.list9.size)
        m.list9.forEach { w.int(it) }
        w.countInt(m.list10.size)
        m.list10.forEach { w.int(it) }
        w.boolean(m.boolean2)
        w.bytes(m.rest)
    }
}
