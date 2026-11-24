package zel.protocol.generated

data class ExtendedProfileOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val string4: String,
    val int2: Int,
    val int3: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val byte1: Byte,
    val list1: List<List1Item>,
    val int4: Int,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val int5: Int,
    val int6: Int,
    val int7: Int,
    val boolean5: Boolean,
    val boolean6: Boolean,
    val int8: Int,
    val int9: Int,
    val list2: List<List2Item>,
    val int10: Int,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val string3: String,
        val string4: String,
        val boolean1: Boolean,
        val int2: Int,
        val boolean2: Boolean,
    )

    data class List2Item(
        val byte1: Byte,
        val int1: Int,
    )
}

object ExtendedProfileOutCodec : OutgoingCodec<ExtendedProfileOut> {
    override val name = "ExtendedProfileOut"

    override fun read(r: PacketReader): ExtendedProfileOut = readExtendedProfileOut(r)

    override fun write(w: PacketWriter, m: ExtendedProfileOut) = writeExtendedProfileOut(w, m)

    private fun readExtendedProfileOutList1Item(r: PacketReader): ExtendedProfileOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vBoolean2 = r.boolean()
        return ExtendedProfileOut.List1Item(vInt1, vString1, vString2, vString3, vString4, vBoolean1, vInt2, vBoolean2)
    }

    private fun readExtendedProfileOutList2Item(r: PacketReader): ExtendedProfileOut.List2Item {
        val vByte1 = r.byte()
        val vInt1 = r.int()
        return ExtendedProfileOut.List2Item(vByte1, vInt1)
    }

    private fun readExtendedProfileOut(r: PacketReader): ExtendedProfileOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vByte1 = r.byte()
        val vList1 = List(r.countInt(18)) { readExtendedProfileOutList1Item(r) }
        val vInt4 = r.int()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vBoolean5 = r.boolean()
        val vBoolean6 = r.boolean()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vList2 = List(r.countInt(5)) { readExtendedProfileOutList2Item(r) }
        val vInt10 = r.int()
        return ExtendedProfileOut(vInt1, vString1, vString2, vString3, vString4, vInt2, vInt3, vBoolean1, vBoolean2, vByte1, vList1, vInt4, vBoolean3, vBoolean4, vInt5, vInt6, vInt7, vBoolean5, vBoolean6, vInt8, vInt9, vList2, vInt10)
    }

    private fun writeExtendedProfileOutList1Item(w: PacketWriter, m: ExtendedProfileOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.boolean(m.boolean2)
    }

    private fun writeExtendedProfileOutList2Item(w: PacketWriter, m: ExtendedProfileOut.List2Item) {
        w.byte(m.byte1)
        w.int(m.int1)
    }

    private fun writeExtendedProfileOut(w: PacketWriter, m: ExtendedProfileOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
        w.int(m.int2)
        w.int(m.int3)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.byte(m.byte1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeExtendedProfileOutList1Item(w, it) }
        w.int(m.int4)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.boolean(m.boolean5)
        w.boolean(m.boolean6)
        w.int(m.int8)
        w.int(m.int9)
        w.countInt(m.list2.size)
        m.list2.forEach { writeExtendedProfileOutList2Item(w, it) }
        w.int(m.int10)
    }
}
