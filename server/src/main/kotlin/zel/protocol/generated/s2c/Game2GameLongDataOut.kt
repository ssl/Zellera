package zel.protocol.generated

data class Game2GameLongDataOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val string2: String,
    val int6: Int,
    val list1: List<List1Item>,
    val trailing1: Trailing1?,
    val trailing2: Int?,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val string3: String,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val int5: Int,
    )

    data class Trailing1(
        val list1: List<List1Item>,
        val int1: Int,
    ) {
        data class List1Item(
            val int1: Int,
            val int2: Int,
        )
    }
}

object Game2GameLongDataOutCodec : OutgoingCodec<Game2GameLongDataOut> {
    override val name = "Game2GameLongDataOut"

    override fun read(r: PacketReader): Game2GameLongDataOut = readGame2GameLongDataOut(r)

    override fun write(w: PacketWriter, m: Game2GameLongDataOut) = writeGame2GameLongDataOut(w, m)

    private fun readGame2GameLongDataOutList1Item(r: PacketReader): Game2GameLongDataOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        return Game2GameLongDataOut.List1Item(vInt1, vString1, vString2, vString3, vInt2, vInt3, vInt4, vInt5)
    }

    private fun readGame2GameLongDataOutTrailing1List1Item(r: PacketReader): Game2GameLongDataOut.Trailing1.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return Game2GameLongDataOut.Trailing1.List1Item(vInt1, vInt2)
    }

    private fun readGame2GameLongDataOutTrailing1(r: PacketReader): Game2GameLongDataOut.Trailing1 {
        val vList1 = List(r.countInt(8)) { readGame2GameLongDataOutTrailing1List1Item(r) }
        val vInt1 = r.int()
        return Game2GameLongDataOut.Trailing1(vList1, vInt1)
    }

    private fun readGame2GameLongDataOut(r: PacketReader): Game2GameLongDataOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vString2 = r.string()
        val vInt6 = r.int()
        val vList1 = List(r.countInt(26)) { readGame2GameLongDataOutList1Item(r) }
        val vTrailing1 = if (r.remaining > 0) readGame2GameLongDataOutTrailing1(r) else null
        val vTrailing2 = if (r.remaining > 0) r.int() else null
        return Game2GameLongDataOut(vInt1, vString1, vInt2, vInt3, vInt4, vInt5, vString2, vInt6, vList1, vTrailing1, vTrailing2)
    }

    private fun writeGame2GameLongDataOutList1Item(w: PacketWriter, m: Game2GameLongDataOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
    }

    private fun writeGame2GameLongDataOutTrailing1List1Item(w: PacketWriter, m: Game2GameLongDataOut.Trailing1.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeGame2GameLongDataOutTrailing1(w: PacketWriter, m: Game2GameLongDataOut.Trailing1) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2GameLongDataOutTrailing1List1Item(w, it) }
        w.int(m.int1)
    }

    private fun writeGame2GameLongDataOut(w: PacketWriter, m: Game2GameLongDataOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.string(m.string2)
        w.int(m.int6)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGame2GameLongDataOutList1Item(w, it) }
        m.trailing1?.let { writeGame2GameLongDataOutTrailing1(w, it) }
        m.trailing2?.let { w.int(it) }
    }
}
