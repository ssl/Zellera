package zel.protocol.generated

data class Game2GameEndingOut(
    val int1: Int,
    val boolean1: Boolean,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
    val int4: Int,
    val int5: Int,
    val trailing1: Int?,
    val trailing2: Int?,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val string1: String,
            val int1: Int,
            val string2: String,
            val string3: String,
            val int2: Int,
            val int3: Int,
            val int4: Int,
            val int5: Int,
            val int6: Int,
            val int7: Int,
            val int8: Int,
            val int9: Int,
            val int10: Int,
            val int11: Int,
            val int12: Int,
        )
    }
}

object Game2GameEndingOutCodec : OutgoingCodec<Game2GameEndingOut> {
    override val name = "Game2GameEndingOut"

    override fun read(r: PacketReader): Game2GameEndingOut = readGame2GameEndingOut(r)

    override fun write(w: PacketWriter, m: Game2GameEndingOut) = writeGame2GameEndingOut(w, m)

    private fun readGame2GameEndingOutList1ItemList1Item(r: PacketReader): Game2GameEndingOut.List1Item.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        val vInt11 = r.int()
        val vInt12 = r.int()
        return Game2GameEndingOut.List1Item.List1Item(vString1, vInt1, vString2, vString3, vInt2, vInt3, vInt4, vInt5, vInt6, vInt7, vInt8, vInt9, vInt10, vInt11, vInt12)
    }

    private fun readGame2GameEndingOutList1Item(r: PacketReader): Game2GameEndingOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(54)) { readGame2GameEndingOutList1ItemList1Item(r) }
        return Game2GameEndingOut.List1Item(vInt1, vInt2, vList1)
    }

    private fun readGame2GameEndingOut(r: PacketReader): Game2GameEndingOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(12)) { readGame2GameEndingOutList1Item(r) }
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        val vTrailing2 = if (r.remaining > 0) r.int() else null
        return Game2GameEndingOut(vInt1, vBoolean1, vInt2, vInt3, vList1, vInt4, vInt5, vTrailing1, vTrailing2)
    }

    private fun writeGame2GameEndingOutList1ItemList1Item(w: PacketWriter, m: Game2GameEndingOut.List1Item.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.int(m.int6)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
        w.int(m.int11)
        w.int(m.int12)
    }

    private fun writeGame2GameEndingOutList1Item(w: PacketWriter, m: Game2GameEndingOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2GameEndingOutList1ItemList1Item(w, it) }
    }

    private fun writeGame2GameEndingOut(w: PacketWriter, m: Game2GameEndingOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2GameEndingOutList1Item(w, it) }
        w.int(m.int4)
        w.int(m.int5)
        m.trailing1?.let { w.int(it) }
        m.trailing2?.let { w.int(it) }
    }
}
