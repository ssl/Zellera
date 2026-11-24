package zel.protocol.generated

data class GuildEditorDataOut(
    val list1: List<List1Item>,
    val list2: List<List2Item>,
    val list3: List<List3Item>,
    val list4: List<List4Item>,
    val list5: List<List5Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
    )

    data class List2Item(
        val int1: Int,
        val string1: String,
        val string2: String,
    )

    data class List3Item(
        val int1: Int,
        val string1: String,
    )

    data class List4Item(
        val int1: Int,
        val string1: String,
    )

    data class List5Item(
        val int1: Int,
        val string1: String,
    )
}

object GuildEditorDataOutCodec : OutgoingCodec<GuildEditorDataOut> {
    override val name = "GuildEditorDataOut"

    override fun read(r: PacketReader): GuildEditorDataOut = readGuildEditorDataOut(r)

    override fun write(w: PacketWriter, m: GuildEditorDataOut) = writeGuildEditorDataOut(w, m)

    private fun readGuildEditorDataOutList1Item(r: PacketReader): GuildEditorDataOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return GuildEditorDataOut.List1Item(vInt1, vString1, vString2)
    }

    private fun readGuildEditorDataOutList2Item(r: PacketReader): GuildEditorDataOut.List2Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return GuildEditorDataOut.List2Item(vInt1, vString1, vString2)
    }

    private fun readGuildEditorDataOutList3Item(r: PacketReader): GuildEditorDataOut.List3Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return GuildEditorDataOut.List3Item(vInt1, vString1)
    }

    private fun readGuildEditorDataOutList4Item(r: PacketReader): GuildEditorDataOut.List4Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return GuildEditorDataOut.List4Item(vInt1, vString1)
    }

    private fun readGuildEditorDataOutList5Item(r: PacketReader): GuildEditorDataOut.List5Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return GuildEditorDataOut.List5Item(vInt1, vString1)
    }

    private fun readGuildEditorDataOut(r: PacketReader): GuildEditorDataOut {
        val vList1 = List(r.countInt(8)) { readGuildEditorDataOutList1Item(r) }
        val vList2 = List(r.countInt(8)) { readGuildEditorDataOutList2Item(r) }
        val vList3 = List(r.countInt(6)) { readGuildEditorDataOutList3Item(r) }
        val vList4 = List(r.countInt(6)) { readGuildEditorDataOutList4Item(r) }
        val vList5 = List(r.countInt(6)) { readGuildEditorDataOutList5Item(r) }
        return GuildEditorDataOut(vList1, vList2, vList3, vList4, vList5)
    }

    private fun writeGuildEditorDataOutList1Item(w: PacketWriter, m: GuildEditorDataOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeGuildEditorDataOutList2Item(w: PacketWriter, m: GuildEditorDataOut.List2Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeGuildEditorDataOutList3Item(w: PacketWriter, m: GuildEditorDataOut.List3Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeGuildEditorDataOutList4Item(w: PacketWriter, m: GuildEditorDataOut.List4Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeGuildEditorDataOutList5Item(w: PacketWriter, m: GuildEditorDataOut.List5Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeGuildEditorDataOut(w: PacketWriter, m: GuildEditorDataOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGuildEditorDataOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeGuildEditorDataOutList2Item(w, it) }
        w.countInt(m.list3.size)
        m.list3.forEach { writeGuildEditorDataOutList3Item(w, it) }
        w.countInt(m.list4.size)
        m.list4.forEach { writeGuildEditorDataOutList4Item(w, it) }
        w.countInt(m.list5.size)
        m.list5.forEach { writeGuildEditorDataOutList5Item(w, it) }
    }
}
