package zel.protocol.generated

data class ShoutOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
    val int4: Int,
    val trailing1: Int?,
    val trailing2: Int?,
    val trailing3: Boolean?,
    val trailing4: Trailing4?,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val boolean1: Boolean,
    )

    data class Trailing4(
        val int1: Int,
        val int2: Int,
    )
}

object ShoutOutCodec : OutgoingCodec<ShoutOut> {
    override val name = "ShoutOut"

    override fun read(r: PacketReader): ShoutOut = readShoutOut(r)

    override fun write(w: PacketWriter, m: ShoutOut) = writeShoutOut(w, m)

    private fun readShoutOutList1Item(r: PacketReader): ShoutOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        return ShoutOut.List1Item(vString1, vString2, vBoolean1)
    }

    private fun readShoutOutTrailing4(r: PacketReader): ShoutOut.Trailing4 {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ShoutOut.Trailing4(vInt1, vInt2)
    }

    private fun readShoutOut(r: PacketReader): ShoutOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(5)) { readShoutOutList1Item(r) }
        val vInt4 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        val vTrailing2 = if (r.remaining > 0) r.int() else null
        val vTrailing3 = if (r.remaining > 0) r.boolean() else null
        val vTrailing4 = if (r.remaining > 0) readShoutOutTrailing4(r) else null
        return ShoutOut(vInt1, vString1, vInt2, vInt3, vList1, vInt4, vTrailing1, vTrailing2, vTrailing3, vTrailing4)
    }

    private fun writeShoutOutList1Item(w: PacketWriter, m: ShoutOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
    }

    private fun writeShoutOutTrailing4(w: PacketWriter, m: ShoutOut.Trailing4) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeShoutOut(w: PacketWriter, m: ShoutOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeShoutOutList1Item(w, it) }
        w.int(m.int4)
        m.trailing1?.let { w.int(it) }
        m.trailing2?.let { w.int(it) }
        m.trailing3?.let { w.boolean(it) }
        m.trailing4?.let { writeShoutOutTrailing4(w, it) }
    }
}
