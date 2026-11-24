package zel.protocol.generated

data class GuildEditInfoOut(
    val list1: List<List1Item>,
    val boolean1: Boolean,
    val int1: Int,
    val string1: String,
    val string2: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val int6: Int,
    val boolean2: Boolean,
    val string3: String,
    val list2: List<List2Item>,
    val string4: String,
    val int7: Int,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val boolean1: Boolean,
    )

    data class List2Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
    )
}

object GuildEditInfoOutCodec : OutgoingCodec<GuildEditInfoOut> {
    override val name = "GuildEditInfoOut"

    override fun read(r: PacketReader): GuildEditInfoOut = readGuildEditInfoOut(r)

    override fun write(w: PacketWriter, m: GuildEditInfoOut) = writeGuildEditInfoOut(w, m)

    private fun readGuildEditInfoOutList1Item(r: PacketReader): GuildEditInfoOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        return GuildEditInfoOut.List1Item(vInt1, vString1, vBoolean1)
    }

    private fun readGuildEditInfoOutList2Item(r: PacketReader): GuildEditInfoOut.List2Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return GuildEditInfoOut.List2Item(vInt1, vInt2, vInt3)
    }

    private fun readGuildEditInfoOut(r: PacketReader): GuildEditInfoOut {
        val vList1 = List(r.countInt(7)) { readGuildEditInfoOutList1Item(r) }
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vBoolean2 = r.boolean()
        val vString3 = r.string()
        val vList2 = List(r.countInt(12)) { readGuildEditInfoOutList2Item(r) }
        val vString4 = r.string()
        val vInt7 = r.int()
        return GuildEditInfoOut(vList1, vBoolean1, vInt1, vString1, vString2, vInt2, vInt3, vInt4, vInt5, vInt6, vBoolean2, vString3, vList2, vString4, vInt7)
    }

    private fun writeGuildEditInfoOutList1Item(w: PacketWriter, m: GuildEditInfoOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
    }

    private fun writeGuildEditInfoOutList2Item(w: PacketWriter, m: GuildEditInfoOut.List2Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeGuildEditInfoOut(w: PacketWriter, m: GuildEditInfoOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGuildEditInfoOutList1Item(w, it) }
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.boolean(m.boolean2)
        w.string(m.string3)
        w.countInt(m.list2.size)
        m.list2.forEach { writeGuildEditInfoOutList2Item(w, it) }
        w.string(m.string4)
        w.int(m.int7)
    }
}
