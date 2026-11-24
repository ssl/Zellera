package zel.protocol.generated

data class PlayerSearchResultOut(
    val list1: List<List1Item>,
    val list2: List<List2Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val boolean1: Boolean,
        val boolean2: Boolean,
        val string3: String,
        val int2: Int,
        val string4: String,
        val string5: String,
    )

    data class List2Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val boolean1: Boolean,
        val boolean2: Boolean,
        val string3: String,
        val int2: Int,
        val string4: String,
        val string5: String,
    )
}

object PlayerSearchResultOutCodec : OutgoingCodec<PlayerSearchResultOut> {
    override val name = "PlayerSearchResultOut"

    override fun read(r: PacketReader): PlayerSearchResultOut = readPlayerSearchResultOut(r)

    override fun write(w: PacketWriter, m: PlayerSearchResultOut) = writePlayerSearchResultOut(w, m)

    private fun readPlayerSearchResultOutList1Item(r: PacketReader): PlayerSearchResultOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vString4 = r.string()
        val vString5 = r.string()
        return PlayerSearchResultOut.List1Item(vInt1, vString1, vString2, vBoolean1, vBoolean2, vString3, vInt2, vString4, vString5)
    }

    private fun readPlayerSearchResultOutList2Item(r: PacketReader): PlayerSearchResultOut.List2Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vString4 = r.string()
        val vString5 = r.string()
        return PlayerSearchResultOut.List2Item(vInt1, vString1, vString2, vBoolean1, vBoolean2, vString3, vInt2, vString4, vString5)
    }

    private fun readPlayerSearchResultOut(r: PacketReader): PlayerSearchResultOut {
        val vList1 = List(r.countInt(20)) { readPlayerSearchResultOutList1Item(r) }
        val vList2 = List(r.countInt(20)) { readPlayerSearchResultOutList2Item(r) }
        return PlayerSearchResultOut(vList1, vList2)
    }

    private fun writePlayerSearchResultOutList1Item(w: PacketWriter, m: PlayerSearchResultOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.string(m.string3)
        w.int(m.int2)
        w.string(m.string4)
        w.string(m.string5)
    }

    private fun writePlayerSearchResultOutList2Item(w: PacketWriter, m: PlayerSearchResultOut.List2Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.string(m.string3)
        w.int(m.int2)
        w.string(m.string4)
        w.string(m.string5)
    }

    private fun writePlayerSearchResultOut(w: PacketWriter, m: PlayerSearchResultOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayerSearchResultOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writePlayerSearchResultOutList2Item(w, it) }
    }
}
