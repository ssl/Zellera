package zel.protocol.generated

data class Game2EnterArenaOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
    val int4: Int,
    val int5: Int,
    val string1: String,
    val list2: List<List2Item>,
    val trailing1: Int?,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val string3: String,
        val int2: Int,
    )

    data class List2Item(
        val string1: String,
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
        val int6: Int,
        val int7: Int,
        val int8: Int,
        val boolean1: Boolean,
        val stuff1: StuffData,
    )
}

object Game2EnterArenaOutCodec : OutgoingCodec<Game2EnterArenaOut> {
    override val name = "Game2EnterArenaOut"

    override fun read(r: PacketReader): Game2EnterArenaOut = readGame2EnterArenaOut(r)

    override fun write(w: PacketWriter, m: Game2EnterArenaOut) = writeGame2EnterArenaOut(w, m)

    private fun readGame2EnterArenaOutList1Item(r: PacketReader): Game2EnterArenaOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        return Game2EnterArenaOut.List1Item(vInt1, vString1, vString2, vString3, vInt2)
    }

    private fun readGame2EnterArenaOutList2Item(r: PacketReader): Game2EnterArenaOut.List2Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vBoolean1 = r.boolean()
        val vStuff1 = StuffData.read(r)
        return Game2EnterArenaOut.List2Item(vString1, vInt1, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vInt8, vBoolean1, vStuff1)
    }

    private fun readGame2EnterArenaOut(r: PacketReader): Game2EnterArenaOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(14)) { readGame2EnterArenaOutList1Item(r) }
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vString1 = r.string()
        val vList2 = List(r.countInt(39)) { readGame2EnterArenaOutList2Item(r) }
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return Game2EnterArenaOut(vInt1, vInt2, vInt3, vList1, vInt4, vInt5, vString1, vList2, vTrailing1)
    }

    private fun writeGame2EnterArenaOutList1Item(w: PacketWriter, m: Game2EnterArenaOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
    }

    private fun writeGame2EnterArenaOutList2Item(w: PacketWriter, m: Game2EnterArenaOut.List2Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.boolean(m.boolean1)
        StuffData.write(w, m.stuff1)
    }

    private fun writeGame2EnterArenaOut(w: PacketWriter, m: Game2EnterArenaOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2EnterArenaOutList1Item(w, it) }
        w.int(m.int4)
        w.int(m.int5)
        w.string(m.string1)
        w.countInt(m.list2.size)
        m.list2.forEach { writeGame2EnterArenaOutList2Item(w, it) }
        m.trailing1?.let { w.int(it) }
    }
}
