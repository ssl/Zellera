package zel.protocol.generated

data class ModeratorInitOut(
    val list1: List<List1Item>,
    val list2: List<String>,
    val list3: List<String>,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val boolean5: Boolean,
    val boolean6: Boolean,
    val boolean7: Boolean,
    val list4: List<String>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val int7: Int,
        val int8: Int,
        val string1: String,
        val int9: Int,
        val string2: String,
        val int10: Int,
        val string3: String,
        val string4: String,
        val int11: Int,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val string1: String,
            val int1: Int,
            val int2: Int,
        )
    }
}

object ModeratorInitOutCodec : OutgoingCodec<ModeratorInitOut> {
    override val name = "ModeratorInitOut"

    override fun read(r: PacketReader): ModeratorInitOut = readModeratorInitOut(r)

    override fun write(w: PacketWriter, m: ModeratorInitOut) = writeModeratorInitOut(w, m)

    private fun readModeratorInitOutList1ItemList1Item(r: PacketReader): ModeratorInitOut.List1Item.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ModeratorInitOut.List1Item.List1Item(vString1, vInt1, vInt2)
    }

    private fun readModeratorInitOutList1Item(r: PacketReader): ModeratorInitOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vString1 = r.string()
        val vInt9 = r.int()
        val vString2 = r.string()
        val vInt10 = r.int()
        val vString3 = r.string()
        val vString4 = r.string()
        val vInt11 = r.int()
        val vList1 = List(r.countInt(10)) { readModeratorInitOutList1ItemList1Item(r) }
        return ModeratorInitOut.List1Item(vInt1, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vInt8, vString1, vInt9, vString2, vInt10, vString3, vString4, vInt11, vList1)
    }

    private fun readModeratorInitOut(r: PacketReader): ModeratorInitOut {
        val vList1 = List(r.countInt(56)) { readModeratorInitOutList1Item(r) }
        val vList2 = List(r.countInt(2)) { r.string() }
        val vList3 = List(r.countInt(2)) { r.string() }
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vBoolean5 = r.boolean()
        val vBoolean6 = r.boolean()
        val vBoolean7 = r.boolean()
        val vList4 = List(r.countInt(2)) { r.string() }
        return ModeratorInitOut(vList1, vList2, vList3, vBoolean1, vBoolean2, vBoolean3, vBoolean4, vBoolean5, vBoolean6, vBoolean7, vList4)
    }

    private fun writeModeratorInitOutList1ItemList1Item(w: PacketWriter, m: ModeratorInitOut.List1Item.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeModeratorInitOutList1Item(w: PacketWriter, m: ModeratorInitOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.string(m.string1)
        w.int(m.int9)
        w.string(m.string2)
        w.int(m.int10)
        w.string(m.string3)
        w.string(m.string4)
        w.int(m.int11)
        w.countInt(m.list1.size)
        m.list1.forEach { writeModeratorInitOutList1ItemList1Item(w, it) }
    }

    private fun writeModeratorInitOut(w: PacketWriter, m: ModeratorInitOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeModeratorInitOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.string(it) }
        w.countInt(m.list3.size)
        m.list3.forEach { w.string(it) }
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.boolean(m.boolean5)
        w.boolean(m.boolean6)
        w.boolean(m.boolean7)
        w.countInt(m.list4.size)
        m.list4.forEach { w.string(it) }
    }
}
